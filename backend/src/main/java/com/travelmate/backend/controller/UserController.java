package com.travelmate.backend.controller;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.service.UserService;
import com.travelmate.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${ADMIN_REGISTER_SECRET:5201314}")
    private String adminRegisterSecret;

    @PostMapping("/register")
    public Result<String> register(
            @RequestParam String username,
            @RequestParam String password) {
        boolean success = userService.register(username, password, 0);
        return success ? Result.success("注册成功") : Result.error("用户名已存在");
    }

    @PostMapping("/admin-register")
    public Result<String> adminRegister(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String secret) {
        if (!adminRegisterSecret.equals(secret)) {
            return Result.error("管理员注册密钥错误");
        }
        boolean success = userService.register(username, password, 1);
        return success ? Result.success("管理员注册成功") : Result.error("用户名已存在或密码不合法");
    }

    @PostMapping("/login")
    public Result<String> login(@RequestParam String username, @RequestParam String password) {
        String token = userService.login(username, password);
        return token != null ? Result.success(token) : Result.error("用户名或密码错误");
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(
            @RequestParam String username,
            @RequestParam String newPassword) {
        boolean ok = userService.resetPassword(username, newPassword);
        return ok ? Result.success("密码重置成功") : Result.error("用户名不存在、新密码不合法或管理员账号不允许重置");
    }

    @PostMapping("/password")
    public Result<String> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        User user = userService.getUserByUsername(username);
        if (user == null) return Result.error("用户不存在");
        boolean ok = userService.changePassword(user.getId(), oldPassword, newPassword);
        return ok ? Result.success("密码修改成功") : Result.error("旧密码错误");
    }

    @DeleteMapping("/account")
    public Result<String> deleteAccount(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String password) {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        User user = userService.getUserByUsername(username);
        if (user == null) return Result.error("用户不存在");
        boolean ok = userService.deleteAccount(user.getId(), password);
        return ok ? Result.success("账户已注销") : Result.error("密码错误");
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
