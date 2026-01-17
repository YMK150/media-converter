package com.mediaconverter.service;

import com.mediaconverter.dto.LoginRequest;
import com.mediaconverter.dto.RegisterRequest;
import com.mediaconverter.dto.ChangePasswordRequest;
import com.mediaconverter.entity.User;
import com.mediaconverter.exception.LogicException;
import com.mediaconverter.repository.UserRepository;
import com.mediaconverter.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @Transactional
    public String login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new LogicException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new LogicException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户登录成功: {}", user.getUsername());
        return generateToken(user);
    }

    /**
     * 用户注册
     */
    @Transactional
    public User register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new LogicException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new LogicException("用户不存在"));

        // 验证当前密码
        if (request.getCurrentPassword() != null &&
            !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new LogicException("当前密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("用户修改密码成功: {}", user.getUsername());
    }

    /**
     * 验证Token并获取用户信息
     */
    public User validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new LogicException("Token无效或已过期");
        }

        if (jwtUtil.isTokenExpired(token)) {
            throw new LogicException("Token已过期，请重新登录");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new LogicException("用户不存在"));
    }

    /**
     * 检查是否有用户存在
     */
    public boolean hasUsers() {
        return userRepository.count() > 0;
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * 生成JWT令牌
     */
    private String generateToken(User user) {
        return jwtUtil.generateToken(user.getUsername(), user.getId());
    }
}
