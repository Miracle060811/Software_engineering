package com.travelmate.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/user")

public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, @RequestParam Integer role) {
        boolean success = userService.register(username, password, role);
        if (success) {
            return "注册成功";
        } else {
            return "用户名已存在";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        String token = userService.login(username, password);
        if (token != null) {
            return token;
        } else {
            return "用户名或密码错误";
        }
    }

    @GetMapping("/me")
    public User getMe(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        return userService.getUserByUsername(username);
    }
}
