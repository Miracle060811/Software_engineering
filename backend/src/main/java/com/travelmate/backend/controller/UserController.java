package com.travelmate.backend.controller;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.service.UserService;
import com.travelmate.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<String> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(defaultValue = "0") Integer role) {
        boolean success = userService.register(username, password, role);
        return success ? Result.success("注册成功") : Result.error("用户名已存在");
    }

    @PostMapping("/login")
    public Result<String> login(@RequestParam String username, @RequestParam String password) {
        String token = userService.login(username, password);
        return token != null ? Result.success(token) : Result.error("用户名或密码错误");
    }

    @GetMapping("/me")
    public Result<User> getMe(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        User user = userService.getUserByUsername(username);
        if (user == null)
            return Result.error("用户不存在");
        user.setPassword(null); // 不返回密码
        return Result.success(user);
    }
}
