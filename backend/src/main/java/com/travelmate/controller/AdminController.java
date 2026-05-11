package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.Post;
import com.travelmate.entity.SysLog;
import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private FlightMapper flightMapper;

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private SysSensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private TrafficOrderMapper trafficOrderMapper;

    // ======================== 权限检查 ========================

    private User checkAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new RuntimeException("无管理员权限");
        }
        return user;
    }

    // ======================== 数据统计 ========================

    /**
     * GET /api/admin/stats - 返回总用户数、总订单数、待审核游记数、今日新增用户
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        checkAdmin();

        long totalUsers = userMapper.selectCount(null);

        long totalOrders = trafficOrderMapper.selectCount(null);

        LambdaQueryWrapper<Post> pendingQuery = new LambdaQueryWrapper<>();
        pendingQuery.eq(Post::getStatus, 0);
        long pendingPosts = postMapper.selectCount(pendingQuery);

        // 今日新增用户：createTime >= 今天00:00:00
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<User> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.ge(User::getCreateTime, todayStart);
        long todayNewUsers = userMapper.selectCount(todayQuery);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalOrders", totalOrders);
        stats.put("pendingPosts", pendingPosts);
        stats.put("todayNewUsers", todayNewUsers);

        return Result.success(stats);
    }

    // ======================== 航班管理 ========================

    /**
     * GET /api/admin/flights - 所有航班列表
     */
    @GetMapping("/flights")
    public Result<List<Flight>> listFlights() {
        checkAdmin();
        return Result.success(flightMapper.selectList(null));
    }

    /**
     * POST /api/admin/flights - 新增航班
     */
    @PostMapping("/flights")
    public Result<Flight> addFlight(@RequestBody Flight flight) {
        checkAdmin();
        flightMapper.insert(flight);
        return Result.success(flight);
    }

    /**
     * PUT /api/admin/flights/{id} - 编辑航班
     */
    @PutMapping("/flights/{id}")
    public Result<Void> updateFlight(@PathVariable Long id, @RequestBody Flight flight) {
        checkAdmin();
        flight.setId(id);
        flightMapper.updateById(flight);
        return Result.success();
    }

    /**
     * DELETE /api/admin/flights/{id} - 删除航班
     */
    @DeleteMapping("/flights/{id}")
    public Result<Void> deleteFlight(@PathVariable Long id) {
        checkAdmin();
        flightMapper.deleteById(id);
        return Result.success();
    }

    // ======================== 酒店管理 ========================

    /**
     * GET /api/admin/hotels - 所有酒店列表
     */
    @GetMapping("/hotels")
    public Result<List<Hotel>> listHotels() {
        checkAdmin();
        return Result.success(hotelMapper.selectList(null));
    }

    /**
     * POST /api/admin/hotels - 新增酒店
     */
    @PostMapping("/hotels")
    public Result<Hotel> addHotel(@RequestBody Hotel hotel) {
        checkAdmin();
        hotelMapper.insert(hotel);
        return Result.success(hotel);
    }

    /**
     * PUT /api/admin/hotels/{id} - 编辑酒店
     */
    @PutMapping("/hotels/{id}")
    public Result<Void> updateHotel(@PathVariable Long id, @RequestBody Hotel hotel) {
        checkAdmin();
        hotel.setId(id);
        hotelMapper.updateById(hotel);
        return Result.success();
    }

    /**
     * DELETE /api/admin/hotels/{id} - 删除酒店
     */
    @DeleteMapping("/hotels/{id}")
    public Result<Void> deleteHotel(@PathVariable Long id) {
        checkAdmin();
        hotelMapper.deleteById(id);
        return Result.success();
    }

    // ======================== 内容审核（游记） ========================

    /**
     * GET /api/admin/posts?status=0 - 游记列表（按状态过滤）
     */
    @GetMapping("/posts")
    public Result<List<Post>> listPosts(@RequestParam(required = false) Integer status) {
        checkAdmin();
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Post::getStatus, status);
        }
        wrapper.orderByDesc(Post::getCreateTime);
        return Result.success(postMapper.selectList(wrapper));
    }

    /**
     * POST /api/admin/posts/{id}/approve - 审核通过（status→1）
     */
    @PostMapping("/posts/{id}/approve")
    public Result<Void> approvePost(@PathVariable Long id) {
        checkAdmin();
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, id).set(Post::getStatus, 1);
        postMapper.update(null, upd);
        return Result.success();
    }

    /**
     * POST /api/admin/posts/{id}/reject - 审核拒绝（status→2）
     */
    @PostMapping("/posts/{id}/reject")
    public Result<Void> rejectPost(@PathVariable Long id) {
        checkAdmin();
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, id).set(Post::getStatus, 2);
        postMapper.update(null, upd);
        return Result.success();
    }

    // ======================== 用户管理 ========================

    /**
     * GET /api/admin/users - 用户列表
     */
    @GetMapping("/users")
    public Result<List<User>> listUsers() {
        checkAdmin();
        return Result.success(userMapper.selectList(null));
    }

    /**
     * POST /api/admin/users/{id}/disable - 禁用用户（status→0）
     */
    @PostMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        checkAdmin();
        LambdaUpdateWrapper<User> upd = new LambdaUpdateWrapper<>();
        upd.eq(User::getId, id).set(User::getStatus, 0);
        userMapper.update(null, upd);
        return Result.success();
    }

    /**
     * POST /api/admin/users/{id}/enable - 启用用户（status→1）
     */
    @PostMapping("/users/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        checkAdmin();
        LambdaUpdateWrapper<User> upd = new LambdaUpdateWrapper<>();
        upd.eq(User::getId, id).set(User::getStatus, 1);
        userMapper.update(null, upd);
        return Result.success();
    }

    // ======================== 敏感词管理 ========================

    /**
     * GET /api/admin/sensitive-words - 敏感词列表
     */
    @GetMapping("/sensitive-words")
    public Result<List<SysSensitiveWord>> listSensitiveWords() {
        checkAdmin();
        return Result.success(sensitiveWordMapper.selectList(null));
    }

    /**
     * POST /api/admin/sensitive-words - 添加敏感词
     */
    @PostMapping("/sensitive-words")
    public Result<SysSensitiveWord> addSensitiveWord(@RequestBody SysSensitiveWord word) {
        checkAdmin();
        word.setCreateTime(LocalDateTime.now());
        sensitiveWordMapper.insert(word);
        return Result.success(word);
    }

    /**
     * DELETE /api/admin/sensitive-words/{id} - 删除敏感词
     */
    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        checkAdmin();
        sensitiveWordMapper.deleteById(id);
        return Result.success();
    }

    // ======================== 系统日志 ========================

    /**
     * GET /api/admin/logs?page=1&size=20 - 操作日志列表
     */
    @GetMapping("/logs")
    public Result<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdmin();
        Page<SysLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysLog::getCreateTime);
        Page<SysLog> result = sysLogMapper.selectPage(pageObj, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }
}
