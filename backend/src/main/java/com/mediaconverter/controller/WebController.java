package com.mediaconverter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 前端页面控制器
 * 负责提供前端页面访问和SPA路由支持
 */
@Controller
@RequestMapping
public class WebController {

    /**
     * 首页访问
     * @return 返回前端入口页面
     */
    @GetMapping("/")
    public String index() {
        return "forward:/static/index.html";
    }

    /**
     * Setup页面访问
     * @return 返回前端入口页面
     */
    @GetMapping("/setup")
    public String setup() {
        return "forward:/static/index.html";
    }

    /**
     * Login页面访问
     * @return 返回前端入口页面
     */
    @GetMapping("/login")
    public String login() {
        return "forward:/static/index.html";
    }

}


