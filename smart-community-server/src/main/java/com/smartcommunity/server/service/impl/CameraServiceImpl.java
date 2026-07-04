package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Camera;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.mapper.CameraMapper;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.service.CameraService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CameraServiceImpl implements CameraService {

    private final CameraMapper cameraMapper;
    private final CommunityMapper communityMapper;

    public CameraServiceImpl(CameraMapper cameraMapper, CommunityMapper communityMapper) {
        this.cameraMapper = cameraMapper;
        this.communityMapper = communityMapper;
    }

    @Override
    public PageResult<Camera> pageList(PageQuery pageQuery, String keyword, Long communityId) {
        Page<Camera> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Camera> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Camera::getName, keyword)
                    .or().like(Camera::getLocation, keyword);
        }
        if (communityId != null) {
            wrapper.eq(Camera::getCommunityId, communityId);
        }
        wrapper.orderByAsc(Camera::getCreateTime);
        Page<Camera> result = cameraMapper.selectPage(page, wrapper);

        // 填充小区名称
        for (Camera c : result.getRecords()) {
            if (c.getCommunityId() != null) {
                Community community = communityMapper.selectById(c.getCommunityId());
                if (community != null) c.setCommunityName(community.getName());
            }
        }

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Camera getById(Long id) {
        return cameraMapper.selectById(id);
    }

    @Override
    @Transactional
    public void add(Camera camera) {
        cameraMapper.insert(camera);
    }

    @Override
    @Transactional
    public void update(Camera camera) {
        cameraMapper.updateById(camera);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        cameraMapper.deleteById(id);
    }
}
