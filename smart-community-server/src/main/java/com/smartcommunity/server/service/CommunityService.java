package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Community;

import java.util.List;

public interface CommunityService {

    PageResult<Community> pageList(PageQuery pageQuery, String keyword);

    Community getById(Long id);

    List<Community> listAll();

    void add(Community community);

    void update(Community community);

    void delete(Long id);
}
