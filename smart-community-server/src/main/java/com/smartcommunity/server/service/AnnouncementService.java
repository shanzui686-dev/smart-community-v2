package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Announcement;

import java.util.List;

public interface AnnouncementService {
    PageResult<Announcement> pageList(PageQuery pageQuery, Long communityId, Integer status, Integer expired, Integer top);
    Announcement getById(Long id);
    List<Announcement> getHomeAnnouncements();
    void add(Announcement announcement);
    void update(Announcement announcement);
    void delete(Long id);
    List<Announcement> listAll();
}
