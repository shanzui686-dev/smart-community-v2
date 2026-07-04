package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Camera;

public interface CameraService {

    PageResult<Camera> pageList(PageQuery pageQuery, String keyword, Long communityId);

    Camera getById(Long id);

    void add(Camera camera);

    void update(Camera camera);

    void delete(Long id);
}
