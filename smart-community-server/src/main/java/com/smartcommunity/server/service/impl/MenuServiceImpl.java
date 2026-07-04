package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.server.entity.Menu;
import com.smartcommunity.server.mapper.MenuMapper;
import com.smartcommunity.server.service.MenuService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final JdbcTemplate jdbcTemplate;

    public MenuServiceImpl(MenuMapper menuMapper, JdbcTemplate jdbcTemplate) {
        this.menuMapper = menuMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Menu> listAll() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getSort);
        return menuMapper.selectList(wrapper);
    }

    @Override
    public Menu getById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    @Transactional
    public void add(Menu menu) {
        menuMapper.insert(menu);
    }

    @Override
    @Transactional
    public void update(Menu menu) {
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        menuMapper.deleteById(id);
        jdbcTemplate.update("DELETE FROM role_menu WHERE menu_id = ?", id);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return jdbcTemplate.queryForList(
                "SELECT menu_id FROM role_menu WHERE role_id = ?", Long.class, roleId);
    }
}
