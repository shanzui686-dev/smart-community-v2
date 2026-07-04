package com.smartcommunity.server.service;

import com.smartcommunity.server.entity.Menu;

import java.util.List;

public interface MenuService {

    List<Menu> listAll();

    Menu getById(Long id);

    void add(Menu menu);

    void update(Menu menu);

    void delete(Long id);

    List<Long> getRoleMenuIds(Long roleId);
}
