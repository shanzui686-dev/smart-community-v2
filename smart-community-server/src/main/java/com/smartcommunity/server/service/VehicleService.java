package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Vehicle;

public interface VehicleService {
    PageResult<Vehicle> pageList(PageQuery pageQuery, String keyword, Long communityId);
    Vehicle getById(Long id);
    void add(Vehicle vehicle);
    void update(Vehicle vehicle);
    void delete(Long id);
}