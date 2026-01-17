package com.mediaconverter.util;

import com.mediaconverter.entity.User;
import com.mediaconverter.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全工具类
 * 用于获取当前登录用户信息
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final AuthService authService;

    /**
     * 获取当前登录用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        String username = getCurrentUsername();
        if (username != null) {
            return authService.getUserByUsername(username);
        }
        return null;
    }

    /**
     * 检查是否已登录
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               !"anonymousUser".equals(authentication.getName());
    }
}
