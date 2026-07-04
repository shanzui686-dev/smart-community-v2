package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.User;

public interface UserService {

    PageResult<User> pageList(PageQuery pageQuery, String keyword, Integer status);

    User getById(Long id);

    void add(User user);

    void update(User user);

    void delete(Long id);

    void updatePassword(Long id, String oldPassword, String newPassword);

    void resetPassword(Long id, String newPassword);
}
