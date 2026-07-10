package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Announcement;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.mapper.AnnouncementMapper;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.service.AnnouncementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final CommunityMapper communityMapper;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper, CommunityMapper communityMapper) {
        this.announcementMapper = announcementMapper;
        this.communityMapper = communityMapper;
    }

    @Override
    public PageResult<Announcement> pageList(PageQuery pageQuery, Long communityId, Integer status, Integer expired, Integer top) {
        Page<Announcement> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (communityId != null) {
            wrapper.eq(Announcement::getCommunityId, communityId);
        }
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }
        if (expired != null) {
            wrapper.eq(Announcement::getExpired, expired);
        }
        if (top != null) {
            wrapper.eq(Announcement::getTop, top);
        }
        wrapper.orderByDesc(Announcement::getTop)
                .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> result = announcementMapper.selectPage(page, wrapper);
        List<Announcement> records = result.getRecords();
        for (Announcement announcement : records) {
            fillExtraInfo(announcement);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public Announcement getById(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            fillExtraInfo(announcement);
        }
        return announcement;
    }

    @Override
    public List<Announcement> getHomeAnnouncements() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getExpired, 0)
                .orderByDesc(Announcement::getTop)
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT 5");
        List<Announcement> announcements = announcementMapper.selectList(wrapper);
        for (Announcement announcement : announcements) {
            fillExtraInfo(announcement);
        }
        return announcements;
    }

    @Override
    @Transactional
    public void add(Announcement announcement) {
        if (announcement.getStatus() == null) {
            announcement.setStatus(0);
        }
        if (announcement.getExpired() == null) {
            announcement.setExpired(0);
        }
        if (announcement.getTop() == null) {
            announcement.setTop(0);
        }
        announcementMapper.insert(announcement);
    }

    @Override
    @Transactional
    public void update(Announcement announcement) {
        announcementMapper.updateById(announcement);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    @Override
    public List<Announcement> listAll() {
        return announcementMapper.selectList(null);
    }

    private void fillExtraInfo(Announcement announcement) {
        if (announcement.getCommunityId() != null) {
            Community community = communityMapper.selectById(announcement.getCommunityId());
            if (community != null) {
                announcement.setCommunityName(community.getName());
            }
        }
    }
}
