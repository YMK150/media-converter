package com.mediaconverter.controller;

import com.mediaconverter.dto.LoginRequest;
import com.mediaconverter.dto.RegisterRequest;
import com.mediaconverter.entity.User;
import com.mediaconverter.exception.LogicException;
import com.mediaconverter.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("message", "登录成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        boolean hasUsers = authService.hasUsers();
        if (hasUsers) {
            throw new LogicException("注册失败,系统已存在用户");
        }
        User user = authService.register(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("message", "注册成功");
        return ResponseEntity.ok(response);
    }


    /**
     * 检查是否有用户存在（用于判断是否显示引导页面）
     */
    @GetMapping("/check-setup")
    public ResponseEntity<Map<String, Object>> checkSetup() {
        boolean hasUsers = authService.hasUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("hasUsers", hasUsers);
        return ResponseEntity.ok(response);
    }

}
