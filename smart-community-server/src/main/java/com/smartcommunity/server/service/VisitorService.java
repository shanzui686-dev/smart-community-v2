package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Visitor;

public interface VisitorService {

    PageResult<Visitor> pageList(PageQuery pageQuery, String name, Long communityId, Integer status);

    Visitor getById(Long id);

    void add(Visitor visitor);

    void update(Visitor visitor);

    void cancel(Long id);

    void checkIn(Long id);

    void checkOut(Long id);
}
