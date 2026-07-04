package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.service.CommunityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityMapper communityMapper;

    public CommunityServiceImpl(CommunityMapper communityMapper) {
        this.communityMapper = communityMapper;
    }

    @Override
    public PageResult<Community> pageList(PageQuery pageQuery, String keyword) {
        Page<Community> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Community> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Community::getName, keyword)
                    .or().like(Community::getAddress, keyword);
        }
        wrapper.orderByAsc(Community::getCreateTime);
        Page<Community> result = communityMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Community getById(Long id) {
        return communityMapper.selectById(id);
    }

    @Override
    public List<Community> listAll() {
        return communityMapper.selectList(null);
    }

    @Override
    @Transactional
    public void add(Community community) {
        communityMapper.insert(community);
    }

    @Override
    @Transactional
    public void update(Community community) {
        communityMapper.updateById(community);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        communityMapper.deleteById(id);
    }
}
