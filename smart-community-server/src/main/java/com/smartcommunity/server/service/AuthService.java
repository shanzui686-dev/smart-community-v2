package com.smartcommunity.server.service;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.dto.LoginDTO;
import com.smartcommunity.server.vo.LoginVO;
import com.smartcommunity.server.vo.MenuTreeVO;
import com.smartcommunity.server.vo.UserInfoVO;

import java.util.List;
import java.util.Map;

public interface AuthService {

    Result<LoginVO> login(LoginDTO loginDTO);

    Result<Void> logout(Long userId);

    Result<UserInfoVO> getUserInfo(Long userId);

    Result<List<MenuTreeVO>> getUserMenus(Long userId);

    Result<Map<String, String>> getCaptcha();
}
