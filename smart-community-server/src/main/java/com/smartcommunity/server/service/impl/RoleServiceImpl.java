package com.smartcommunity.server.service.impl;

import com.smartcommunity.server.entity.Role;
import com.smartcommunity.server.mapper.RoleMapper;
import com.smartcommunity.server.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Role> listAll() {
        return roleMapper.selectList(null);
    }

    @Override
    public Role getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    @Transactional
    public void add(Role role) {
        roleMapper.insert(role);
    }

    @Override
    @Transactional
    public void update(Role role) {
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        roleMapper.deleteById(id);
        jdbcTemplate.update("DELETE FROM role_menu WHERE role_id = ?", id);
        jdbcTemplate.update("DELETE FROM user_role WHERE role_id = ?", id);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        jdbcTemplate.update("DELETE FROM role_menu WHERE role_id = ?", roleId);
        for (Long menuId : menuIds) {
            jdbcTemplate.update("INSERT INTO role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        }
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return jdbcTemplate.queryForList(
                "SELECT menu_id FROM role_menu WHERE role_id = ?", Long.class, roleId);
    }
}
