package com.alibaba.work.faas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 后台管理页面路由。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("")
    public String index() {
        return "admin";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
