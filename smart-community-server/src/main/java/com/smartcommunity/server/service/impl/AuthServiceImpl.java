package com.smartcommunity.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.dto.LoginDTO;
import com.smartcommunity.server.entity.Menu;
import com.smartcommunity.server.entity.User;
import com.smartcommunity.server.mapper.MenuMapper;
import com.smartcommunity.server.mapper.UserMapper;
import com.smartcommunity.server.security.JwtUtil;
import com.smartcommunity.server.service.AuthService;
import com.smartcommunity.server.service.CaptchaService;
import com.smartcommunity.server.utils.RedisUtil;
import com.smartcommunity.server.vo.LoginVO;
import com.smartcommunity.server.vo.MenuTreeVO;
import com.smartcommunity.server.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;

    @Override
    public Result<LoginVO> login(LoginDTO loginDTO) {
        // 验证码校验
        if (!captchaService.validate(loginDTO.getCaptchaKey(), loginDTO.getCaptcha())) {
            return Result.error("验证码错误或已过期");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) return Result.error("用户名或密码错误");
        if (user.getStatus() == 0) return Result.error("账号已被禁用");
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword()))
            return Result.error("用户名或密码错误");

        List<String> roles = userMapper.selectRoleNamesByUserId(user.getUserId());
        List<String> permissions = userMapper.selectPermissionsByUserId(user.getUserId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), claims);
        try {
            redisUtil.set("token:" + user.getUserId(), token, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            // Redis不可用时忽略，不影响登录
        }

        LoginVO loginVO = LoginVO.builder()
                .token(token).userId(user.getUserId()).username(user.getUsername())
                .realName(user.getRealName()).avatar(user.getAvatar())
                .roles(roles).permissions(permissions).build();
        return Result.success("登录成功", loginVO);
    }

    @Override
    public Result<Void> logout(Long userId) {
        redisUtil.delete("token:" + userId);
        return Result.success();
    }

    @Override
    public Result<UserInfoVO> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        vo.setRoles(userMapper.selectRoleNamesByUserId(userId));
        vo.setPermissions(userMapper.selectPermissionsByUserId(userId));
        return Result.success(vo);
    }

    @Override
    public Result<List<MenuTreeVO>> getUserMenus(Long userId) {
        List<Menu> menuList = menuMapper.selectMenusByUserId(userId);
        List<MenuTreeVO> treeList = menuList.stream().map(menu -> {
            MenuTreeVO vo = new MenuTreeVO();
            BeanUtil.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(buildMenuTree(treeList, 0L));
    }

    @Override
    public Result<Map<String, String>> getCaptcha() {
        return Result.success(captchaService.generate());
    }

    private List<MenuTreeVO> buildMenuTree(List<MenuTreeVO> list, Long parentId) {
        return list.stream()
                .filter(menu -> menu.getParentId().equals(parentId))
                .sorted(Comparator.comparingInt(MenuTreeVO::getSort))
                .map(menu -> { menu.setChildren(buildMenuTree(list, menu.getMenuId())); return menu; })
                .collect(Collectors.toList());
    }
}
