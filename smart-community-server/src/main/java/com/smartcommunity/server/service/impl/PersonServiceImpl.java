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

@Service
public class PersonServiceImpl implements PersonService {

    private final PersonMapper personMapper;
    private final CommunityMapper communityMapper;
    private final OssService ossService;
    private final FaceRecognitionService faceRecognitionService;

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
        // 如果新增时已有人脸照片，注册到人脸识别服务
        if (person.getFaceUrl() != null && !person.getFaceUrl().isBlank()) {
            String faceId = faceRecognitionService.registerFace(person.getFaceUrl(), String.valueOf(person.getPersonId()));
            if (faceId != null) {
                person.setFaceId(faceId);
                personMapper.updateById(person);
            }
        }
    }

    @Override
    @Transactional
    public void update(Person person) {
        // 如果更新了人脸照片，重新注册到人脸库
        if (person.getFaceUrl() != null && !person.getFaceUrl().isBlank()) {
            Person old = personMapper.selectById(person.getPersonId());
            // 人脸照片变更了，先删旧的人脸再注册新的
            if (old != null && old.getFaceId() != null && !person.getFaceUrl().equals(old.getFaceUrl())) {
                faceRecognitionService.deleteFace(old.getFaceId(), String.valueOf(person.getPersonId()));
            }
            String faceId = faceRecognitionService.registerFace(person.getFaceUrl(), String.valueOf(person.getPersonId()));
            person.setFaceId(faceId);
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
