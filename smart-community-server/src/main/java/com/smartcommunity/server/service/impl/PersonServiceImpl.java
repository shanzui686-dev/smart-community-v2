package com.smartcommunity.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.mapper.PersonMapper;
import com.smartcommunity.server.service.FaceRecognitionService;
import com.smartcommunity.server.service.OssService;
import com.smartcommunity.server.service.PersonService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PersonServiceImpl implements PersonService {

    private final PersonMapper personMapper;
    private final CommunityMapper communityMapper;
    private final OssService ossService;
    private final FaceRecognitionService faceRecognitionService;

    /** 人脸操作锁，防止并发注册同一张人脸（百度 AI 注册非即时生效） */
    private final Object faceLock = new Object();

    public PersonServiceImpl(PersonMapper personMapper, CommunityMapper communityMapper,
                             OssService ossService, FaceRecognitionService faceRecognitionService) {
        this.personMapper = personMapper;
        this.communityMapper = communityMapper;
        this.ossService = ossService;
        this.faceRecognitionService = faceRecognitionService;
    }

    @Override
    public PageResult<Person> pageList(PageQuery pageQuery, String keyword, Long communityId, Integer personType) {
        Page<Person> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Person> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Person::getUserName, keyword)
                    .or().like(Person::getMobile, keyword)
                    .or().like(Person::getHouseNo, keyword));
        }
        if (communityId != null) {
            wrapper.eq(Person::getCommunityId, communityId);
        }
        if (personType != null) {
            wrapper.eq(Person::getPersonType, personType);
        }
        wrapper.orderByAsc(Person::getCreateTime);
        Page<Person> result = personMapper.selectPage(page, wrapper);
        // 填充小区名称
        for (Person person : result.getRecords()) {
            if (person.getCommunityId() != null) {
                Community community = communityMapper.selectById(person.getCommunityId());
                if (community != null) {
                    person.setCommunityName(community.getName());
                }
            }
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Person getById(Long id) {
        return personMapper.selectById(id);
    }

    @Override
    @Transactional
    public void add(Person person) {
        personMapper.insert(person);

        if (person.getFaceUrl() != null && !person.getFaceUrl().isBlank()) {
            synchronized (faceLock) {
                // 1. 注册前查重（同时校验DB，过滤已删除居民的幽灵数据）
                Map<String, Object> before = faceRecognitionService.searchFace(person.getFaceUrl());
                if (before != null) {
                    String foundId = String.valueOf(before.get("personId"));
                    double score = getScore(before);
                    if (score > 90 && isFaceStillValid(foundId)) {
                        throw new RuntimeException("该人脸已存在（置信度 " + String.format("%.2f", score) + "%），请勿重复录入");
                    }
                }
                // 2. 注册（失败会抛异常回滚事务）
                String faceId = faceRecognitionService.registerFace(person.getFaceUrl(), String.valueOf(person.getPersonId()));
                person.setFaceId(faceId);
                personMapper.updateById(person);
                // 3. 注册后自查
                Map<String, Object> after = faceRecognitionService.searchFace(person.getFaceUrl());
                if (after != null) {
                    String foundId = String.valueOf(after.get("personId"));
                    String myId = String.valueOf(person.getPersonId());
                    if (!foundId.equals(myId) && getScore(after) > 90 && isFaceStillValid(foundId)) {
                        if (faceId != null) {
                            faceRecognitionService.deleteFace(faceId, String.valueOf(person.getPersonId()));
                        }
                        throw new RuntimeException("该人脸已被其他居民使用，请勿重复录入");
                    }
                }
            }
        }
    }

    private double getScore(Map<String, Object> result) {
        Object s = result.get("score");
        return s instanceof Number ? ((Number) s).doubleValue() : 0;
    }

    /** 校验百度 AI 搜到的人脸在数据库中是否仍然有效（未被删除/未清空人脸） */
    private boolean isFaceStillValid(String personIdFromBaidu) {
        try {
            Long id = Long.valueOf(personIdFromBaidu);
            Person p = personMapper.selectById(id);
            return p != null
                && p.getFaceUrl() != null && !p.getFaceUrl().isBlank()
                && p.getFaceId() != null && !p.getFaceId().isBlank();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void update(Person person) {
        if (person.getFaceUrl() != null && !person.getFaceUrl().isBlank()) {
            Person old = personMapper.selectById(person.getPersonId());
            boolean faceChanged = (old == null || !person.getFaceUrl().equals(old.getFaceUrl()));

            if (faceChanged) {
                synchronized (faceLock) {
                    String myId = String.valueOf(person.getPersonId());
                    // 1. 注册前查重（校验DB，过滤幽灵数据）
                    Map<String, Object> before = faceRecognitionService.searchFace(person.getFaceUrl());
                    if (before != null) {
                        String foundId = String.valueOf(before.get("personId"));
                        if (!foundId.equals(myId) && getScore(before) > 90 && isFaceStillValid(foundId)) {
                            throw new RuntimeException("该人脸已被其他居民使用（居民 ID: " + foundId
                                    + "，置信度 " + String.format("%.2f", getScore(before)) + "%），请勿重复录入");
                        }
                    }
                    // 2. 删旧
                    if (old != null && old.getFaceId() != null) {
                        faceRecognitionService.deleteFace(old.getFaceId(), myId);
                    }
                    // 3. 注册新（失败会抛异常回滚）
                    String faceId = faceRecognitionService.registerFace(person.getFaceUrl(), myId);
                    person.setFaceId(faceId);
                    // 4. 注册后自查
                    Map<String, Object> after = faceRecognitionService.searchFace(person.getFaceUrl());
                    if (after != null && faceId != null) {
                        String foundId = String.valueOf(after.get("personId"));
                        if (!foundId.equals(myId) && getScore(after) > 90 && isFaceStillValid(foundId)) {
                            faceRecognitionService.deleteFace(faceId, myId);
                            throw new RuntimeException("该人脸已被其他居民使用，请勿重复录入");
                        }
                    }
                }
            }
        }
        personMapper.updateById(person);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Person person = personMapper.selectById(id);
        if (person != null && person.getFaceId() != null) {
            faceRecognitionService.deleteFace(person.getFaceId(), String.valueOf(id));
        }
        personMapper.deleteById(id);
    }

    @Override
    public String uploadFace(Long personId, MultipartFile file) {
        try {
            // 1. 上传到 OSS
            String faceUrl = ossService.upload(file, "face", "face");
            // 2. 注册到人脸识别服务
            String faceId = faceRecognitionService.registerFace(faceUrl, String.valueOf(personId));
            // 3. 更新居民记录
            Person person = personMapper.selectById(personId);
            if (person != null) {
                person.setFaceUrl(faceUrl);
                person.setFaceId(faceId);
                personMapper.updateById(person);
            }
            return faceUrl;
        } catch (Exception e) {
            throw new RuntimeException("人脸照片上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void importExcel(MultipartFile file) {
        try {
            List<Person> personList = new ArrayList<>();
            EasyExcel.read(file.getInputStream(), Person.class, new PageReadListener<Person>(dataList -> {
                for (Person person : dataList) {
                    personMapper.insert(person);
                }
            })).sheet().doRead();
        } catch (IOException e) {
            throw new RuntimeException("Excel导入失败: " + e.getMessage());
        }
    }

    @Override
    public void exportExcel(HttpServletResponse response, String keyword, Long communityId, Integer personType, String ids) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("居民信息", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            LambdaQueryWrapper<Person> wrapper = new LambdaQueryWrapper<>();
            // 优先按选中ID导出
            if (ids != null && !ids.isBlank()) {
                List<Long> idList = new ArrayList<>();
                for (String id : ids.split(",")) {
                    idList.add(Long.parseLong(id.trim()));
                }
                wrapper.in(Person::getPersonId, idList);
            } else {
                if (keyword != null && !keyword.isBlank()) {
                    wrapper.and(w -> w.like(Person::getUserName, keyword)
                            .or().like(Person::getMobile, keyword)
                            .or().like(Person::getHouseNo, keyword));
                }
                if (communityId != null) {
                    wrapper.eq(Person::getCommunityId, communityId);
                }
                if (personType != null) {
                    wrapper.eq(Person::getPersonType, personType);
                }
            }
            wrapper.orderByAsc(Person::getCreateTime);

            List<Person> list = personMapper.selectList(wrapper);
            // 填充小区名称
            for (Person p : list) {
                if (p.getCommunityId() != null) {
                    Community c = communityMapper.selectById(p.getCommunityId());
                    if (c != null) p.setCommunityName(c.getName());
                }
            }
            EasyExcel.write(response.getOutputStream(), Person.class)
                    .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                    .sheet("居民信息")
                    .doWrite(list);
        } catch (IOException e) {
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        }
    }
}
