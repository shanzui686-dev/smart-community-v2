package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.User;
import com.smartcommunity.server.mapper.UserMapper;
import com.smartcommunity.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public PageResult<User> pageList(PageQuery pageQuery, String keyword, Integer status) {
        Page<User> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword).or().like(User::getMobile, keyword));
        }
        if (status != null) wrapper.eq(User::getStatus, status);
        wrapper.orderByAsc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setRoleIds(userMapper.selectRoleIdsByUserId(id));
        }
        return user;
    }

    @Override
    @Transactional
    public void add(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        // 保存用户角色关联
        if (!CollectionUtils.isEmpty(user.getRoleIds())) {
            user.getRoleIds().forEach(roleId -> userMapper.insertUserRole(user.getUserId(), roleId));
        }
    }

    @Override
    @Transactional
    public void update(User user) {
        user.setPassword(null);
        userMapper.updateById(user);
        // 同步用户角色关联：先删后插
        userMapper.deleteUserRolesByUserId(user.getUserId());
        if (!CollectionUtils.isEmpty(user.getRoleIds())) {
            user.getRoleIds().forEach(roleId -> userMapper.insertUserRole(user.getUserId(), roleId));
        }
    }

    @Override
    @Transactional
    public void delete(Long id) { userMapper.deleteById(id); }

    @Override
    @Transactional
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) throw new RuntimeException("原密码错误");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
