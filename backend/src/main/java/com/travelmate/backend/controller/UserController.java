package com.travelmate.backend.controller;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.service.AdminBootstrapService;
import com.travelmate.backend.service.RefreshTokenService;
import com.travelmate.backend.service.UserService;
import com.travelmate.common.Result;
import com.travelmate.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminBootstrapService adminBootstrapService;
    @Autowired
    private RefreshTokenService refreshTokenService;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    private static final String REFRESH_COOKIE = "TRAVELMATE_REFRESH";

    @PostMapping("/register")
    @RateLimiter(maxRequests = 5, timeWindowSeconds = 60)
    public Result<String> register(
            @RequestParam String username,
            @RequestParam String password) {
        boolean success = userService.register(username, password, 0);
        return success ? Result.success("注册成功") : Result.error("用户名已存在");
    }

    @PostMapping("/reset-password")
    @RateLimiter(maxRequests = 5, timeWindowSeconds = 60)
    public Result<String> resetPassword(
            @RequestParam String username,
            @RequestParam String newPassword) {
        boolean ok = userService.resetPassword(username, newPassword);
        return ok ? Result.success("密码重置成功") : Result.error("用户名不存在、新密码不合法或管理员账号不允许重置");
    }

    @PostMapping("/admin-register")
    @RateLimiter(maxRequests = 5, timeWindowSeconds = 300)
    public Result<String> adminRegister(@RequestBody AdminRegisterRequest request, HttpServletRequest servletRequest) {
        if (request == null) {
            return Result.error("管理员初始化不可用");
        }
        String error = adminBootstrapService.register(
                request.username(), request.password(), request.secret(), servletRequest.getRemoteAddr());
        return error == null ? Result.success("管理员注册成功") : Result.error(error);
    }

    @PostMapping("/login")
    @RateLimiter(maxRequests = 10, timeWindowSeconds = 60)
    public Result<String> login(@RequestParam String username, @RequestParam String password,
                                HttpServletRequest request, HttpServletResponse response) {
        String token = userService.login(username, password);
        if (token == null) return Result.error("用户名或密码错误");
        User user = userService.getUserByUsername(username.trim());
        RefreshTokenService.RefreshGrant grant = refreshTokenService.create(
                user, request.getRemoteAddr(), request.getHeader("User-Agent"));
        setRefreshCookie(response, grant.refreshToken(), refreshTokenService.getRefreshDays());
        return Result.success(grant.accessToken());
    }

    @PostMapping("/refresh")
    public Result<String> refresh(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                  HttpServletRequest request, HttpServletResponse response) {
        RefreshTokenService.RefreshGrant grant = refreshTokenService.rotate(
                refreshToken, request.getRemoteAddr(), request.getHeader("User-Agent"));
        if (grant == null) {
            clearRefreshCookie(response);
            return Result.error("登录状态已失效");
        }
        setRefreshCookie(response, grant.refreshToken(), refreshTokenService.getRefreshDays());
        return Result.success(grant.accessToken());
    }

    @PostMapping("/logout")
    public Result<String> logout(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                 HttpServletResponse response) {
        refreshTokenService.revoke(refreshToken);
        clearRefreshCookie(response);
        return Result.success("已退出登录");
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

    public record AdminRegisterRequest(String username, String password, String secret) {
    }

    private void setRefreshCookie(HttpServletResponse response, String value, int days) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/user")
                .maxAge(Duration.ofDays(days))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/user")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
