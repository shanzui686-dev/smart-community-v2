package com.smartcommunity.server.service;

import com.smartcommunity.server.entity.Role;

import java.util.List;

public interface RoleService {

    List<Role> listAll();

    Role getById(Long id);

    void add(Role role);

    void update(Role role);

    void delete(Long id);

    void assignMenus(Long roleId, List<Long> menuIds);

    List<Long> getRoleMenuIds(Long roleId);
}
