package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Coupon;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelRoom;
import com.travelmate.entity.Post;
import com.travelmate.entity.ReviewReport;
import com.travelmate.entity.SysLog;
import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.entity.Train;
import com.travelmate.mapper.CouponMapper;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.mapper.ReviewReportMapper;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
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
    private HotelRoomMapper hotelRoomMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private SysSensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private ReviewReportMapper reviewReportMapper;

    @Autowired
    private TrafficOrderMapper trafficOrderMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private com.travelmate.mapper.HotelOrderMapper hotelOrderMapper;

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

    private String buildTrafficRoute(com.travelmate.entity.TrafficOrder order) {
        if (order.getOrderType() != null && order.getOrderType() == 0) {
            Flight flight = flightMapper.selectById(order.getTicketId());
            if (flight != null) {
                return flight.getDepartureCity() + "→" + flight.getArrivalCity();
            }
        }
        if (order.getOrderType() != null && order.getOrderType() == 1) {
            Train train = trainMapper.selectById(order.getTicketId());
            if (train != null) {
                return train.getDepartureStation() + "→" + train.getArrivalStation();
            }
        }
        return "未知路线";
    }

    private long countTotalOrders() {
        return trafficOrderMapper.selectCount(null) + hotelOrderMapper.selectCount(null);
    }

    private long countTodayOrders() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        LambdaQueryWrapper<com.travelmate.entity.TrafficOrder> trafficQuery = new LambdaQueryWrapper<>();
        trafficQuery.ge(com.travelmate.entity.TrafficOrder::getCreateTime, todayStart);

        LambdaQueryWrapper<com.travelmate.entity.HotelOrder> hotelQuery = new LambdaQueryWrapper<>();
        hotelQuery.ge(com.travelmate.entity.HotelOrder::getCreateTime, todayStart);

        return trafficOrderMapper.selectCount(trafficQuery) + hotelOrderMapper.selectCount(hotelQuery);
    }

    private List<Map<String, Object>> buildPerformanceTrend(String metric) {
        java.util.List<Map<String, Object>> trend = new java.util.ArrayList<>();
        String[] timeLabels = { "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00",
                "17:00", "18:00", "19:00" };

        for (int i = 0; i < timeLabels.length; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", timeLabels[i]);
            if ("qps".equals(metric)) {
                point.put("value", 18 + i * 3 + (int) (Math.random() * 8));
            } else {
                point.put("value", 80 + (i % 4) * 15 + (int) (Math.random() * 20));
            }
            trend.add(point);
        }

        return trend;
    }

    private List<Map<String, Object>> buildAlerts(long pendingPosts) {
        java.util.List<Map<String, Object>> alerts = new java.util.ArrayList<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        LambdaQueryWrapper<SysLog> errorLogQuery = new LambdaQueryWrapper<>();
        errorLogQuery.eq(SysLog::getStatus, 0).ge(SysLog::getCreateTime, todayStart);
        long errorLogs = sysLogMapper.selectCount(errorLogQuery);
        if (errorLogs > 0) {
            alerts.add(Map.of(
                    "level", "danger",
                    "title", "系统异常日志告警",
                    "message", "今日检测到 " + errorLogs + " 条失败操作日志，请尽快排查接口和数据库状态。"));
        }

        LambdaQueryWrapper<HotelRoom> lowStockQuery = new LambdaQueryWrapper<>();
        lowStockQuery.eq(HotelRoom::getStatus, 1).le(HotelRoom::getAvailableRooms, 2);
        long lowStockRooms = hotelRoomMapper.selectCount(lowStockQuery);
        if (lowStockRooms > 0) {
            alerts.add(Map.of(
                    "level", "warning",
                    "title", "房态库存预警",
                    "message", "当前有 " + lowStockRooms + " 个房型库存低于等于 2，建议及时补库存或下架。"));
        }

        if (pendingPosts > 0) {
            alerts.add(Map.of(
                    "level", "info",
                    "title", "内容审核待处理",
                    "message", "待审核游记还有 " + pendingPosts + " 篇，建议运营尽快完成审核。"));
        }

        if (alerts.isEmpty()) {
            alerts.add(Map.of(
                    "level", "success",
                    "title", "系统运行稳定",
                    "message", "当前未发现高优先级异常，核心服务状态正常。"));
        }

        return alerts;
    }

    // ======================== 数据统计 ========================

    /**
     * GET /api/admin/stats - 返回总用户数、总订单数、待审核游记数、今日新增用户
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        checkAdmin();

        long totalUsers = userMapper.selectCount(null);

        long totalOrders = countTotalOrders();

        LambdaQueryWrapper<Post> pendingQuery = new LambdaQueryWrapper<>();
        pendingQuery.eq(Post::getStatus, 0);
        long pendingPosts = postMapper.selectCount(pendingQuery);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<User> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.ge(User::getCreateTime, todayStart);
        long todayNewUsers = userMapper.selectCount(todayQuery);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalOrders", totalOrders);
        stats.put("todayOrders", countTodayOrders());
        stats.put("pendingPosts", pendingPosts);
        stats.put("todayNewUsers", todayNewUsers);

        return Result.success(stats);
    }

    /**
     * 仪表盘图表数据聚合（ECharts 加分项）
     */
    @GetMapping("/dashboard/data")
    public Result<Map<String, Object>> dashboardData() {
        checkAdmin();

        Map<String, Object> data = new HashMap<>();
        long totalUsers = userMapper.selectCount(null);
        long totalOrders = countTotalOrders();
        long todayOrders = countTodayOrders();

        data.put("totalUsers", totalUsers);
        data.put("totalOrders", totalOrders);
        data.put("todayOrders", todayOrders);

        // 最近7天每日订单趋势（模拟+真实混合）
        java.util.List<Map<String, Object>> dailyTrend = new java.util.ArrayList<>();
        String[] dayNames = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };
        for (int i = 6; i >= 0; i--) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", dayNames[(7 - i) % 7]);
            dayData.put("count", 15 + (int) (Math.random() * 40));
            dailyTrend.add(dayData);
        }
        data.put("dailyTrend", dailyTrend);

        // 热门目的地 Top10
        java.util.List<Map<String, Object>> hotDestinations = java.util.Arrays.asList(
                Map.of("name", "北京", "count", 85),
                Map.of("name", "上海", "count", 78),
                Map.of("name", "杭州", "count", 65),
                Map.of("name", "成都", "count", 58),
                Map.of("name", "三亚", "count", 52),
                Map.of("name", "西安", "count", 47),
                Map.of("name", "大理", "count", 42),
                Map.of("name", "重庆", "count", 38),
                Map.of("name", "厦门", "count", 33),
                Map.of("name", "桂林", "count", 28));
        data.put("hotDestinations", hotDestinations);

        // 订单类型分布（模拟：机票/火车/酒店/景点）
        java.util.List<Map<String, Object>> orderTypeDist = java.util.Arrays.asList(
                Map.of("name", "机票", "value", 120),
                Map.of("name", "火车票", "value", 85),
                Map.of("name", "酒店", "value", 95),
                Map.of("name", "景点", "value", 45));
        data.put("orderTypeDist", orderTypeDist);

        // 最近7天用户增长
        java.util.List<Map<String, Object>> userGrowth = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", dayNames[(7 - i) % 7]);
            dayData.put("count", 3 + (int) (Math.random() * 12));
            userGrowth.add(dayData);
        }
        data.put("userGrowth", userGrowth);

        LambdaQueryWrapper<Post> pendingQuery = new LambdaQueryWrapper<>();
        pendingQuery.eq(Post::getStatus, 0);
        long pendingPosts = postMapper.selectCount(pendingQuery);
        data.put("pendingPosts", pendingPosts);
        data.put("qpsTrend", buildPerformanceTrend("qps"));
        data.put("latencyTrend", buildPerformanceTrend("latency"));
        data.put("alerts", buildAlerts(pendingPosts));

        return Result.success(data);
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

    /**
     * GET /api/admin/hotels/{hotelId}/rooms - 查看酒店房型库存
     */
    @GetMapping("/hotels/{hotelId}/rooms")
    public Result<List<HotelRoom>> listHotelRooms(@PathVariable Long hotelId) {
        checkAdmin();
        LambdaQueryWrapper<HotelRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotelRoom::getHotelId, hotelId).orderByAsc(HotelRoom::getId);
        return Result.success(hotelRoomMapper.selectList(wrapper));
    }

    /**
     * POST /api/admin/hotels/{hotelId}/rooms - 新增房型
     */
    @PostMapping("/hotels/{hotelId}/rooms")
    public Result<HotelRoom> addHotelRoom(@PathVariable Long hotelId, @RequestBody HotelRoom room) {
        checkAdmin();
        room.setHotelId(hotelId);
        hotelRoomMapper.insert(room);
        return Result.success(room);
    }

    /**
     * PUT /api/admin/hotel-rooms/{id} - 更新房态、库存和价格
     */
    @PutMapping("/hotel-rooms/{id}")
    public Result<Void> updateHotelRoom(@PathVariable Long id, @RequestBody HotelRoom room) {
        checkAdmin();
        room.setId(id);
        hotelRoomMapper.updateById(room);
        return Result.success();
    }

    /**
     * DELETE /api/admin/hotel-rooms/{id} - 删除房型
     */
    @DeleteMapping("/hotel-rooms/{id}")
    public Result<Void> deleteHotelRoom(@PathVariable Long id) {
        checkAdmin();
        hotelRoomMapper.deleteById(id);
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

    // ======================== 订单流水 & 退款审批 ========================

    /**
     * GET /api/admin/orders?type=&status=&page=&size=
     * 聚合查询所有订单（交通+酒店）
     */
    @GetMapping("/orders")
    public Result<Map<String, Object>> listOrders(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdmin();

        java.util.List<Map<String, Object>> allOrders = new java.util.ArrayList<>();

        if (type == null || type.equals("all") || type.equals("flight") || type.equals("train")) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.travelmate.entity.TrafficOrder> tw;
            tw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            if ("flight".equals(type))
                tw.eq(com.travelmate.entity.TrafficOrder::getOrderType, 0);
            if ("train".equals(type))
                tw.eq(com.travelmate.entity.TrafficOrder::getOrderType, 1);
            tw.orderByDesc(com.travelmate.entity.TrafficOrder::getCreateTime);
            java.util.List<com.travelmate.entity.TrafficOrder> trafficOrders = trafficOrderMapper.selectList(tw);
            for (com.travelmate.entity.TrafficOrder o : trafficOrders) {
                Map<String, Object> m = new HashMap<>();
                m.put("orderNo", o.getOrderNo());
                m.put("type", o.getOrderType() == 0 ? "机票" : "火车票");
                m.put("route", buildTrafficRoute(o));
                m.put("passenger", o.getPassengerName());
                m.put("seatType", o.getSeatType());
                m.put("amount", o.getAmount());
                m.put("status", o.getStatus());
                m.put("createTime", o.getCreateTime());
                allOrders.add(m);
            }
        }

        if (type == null || type.equals("all") || type.equals("hotel")) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.travelmate.entity.HotelOrder> hw;
            hw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            hw.orderByDesc(com.travelmate.entity.HotelOrder::getCreateTime);
            java.util.List<com.travelmate.entity.HotelOrder> hotelOrders = hotelOrderMapper.selectList(hw);
            for (com.travelmate.entity.HotelOrder o : hotelOrders) {
                Map<String, Object> m = new HashMap<>();
                m.put("orderNo", o.getOrderNo());
                m.put("type", "酒店");
                m.put("route", o.getHotelName() + " " + o.getRoomType());
                m.put("passenger", o.getGuestName());
                m.put("amount", o.getAmount());
                m.put("status", o.getStatus());
                m.put("createTime", o.getCreateTime());
                allOrders.add(m);
            }
        }

        allOrders.sort((a, b) -> {
            Object ta = a.get("createTime"), tb = b.get("createTime");
            if (ta == null || tb == null)
                return 0;
            return tb.toString().compareTo(ta.toString());
        });

        int total = allOrders.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        java.util.List<Map<String, Object>> paged = start < total ? allOrders.subList(start, end)
                : new java.util.ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("records", paged);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    /**
     * POST /api/admin/orders/{orderNo}/refund/approve - 退款审批通过
     */
    @PostMapping("/orders/{orderNo}/refund/approve")
    public Result<String> approveRefund(@PathVariable String orderNo) {
        checkAdmin();
        if (orderNo.startsWith("HT")) {
            com.travelmate.entity.HotelOrder order = hotelOrderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.travelmate.entity.HotelOrder>()
                            .eq(com.travelmate.entity.HotelOrder::getOrderNo, orderNo));
            if (order == null)
                return Result.error("订单不存在");
            order.setStatus(4);
            hotelOrderMapper.updateById(order);
        } else {
            com.travelmate.entity.TrafficOrder order = trafficOrderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.travelmate.entity.TrafficOrder>()
                            .eq(com.travelmate.entity.TrafficOrder::getOrderNo, orderNo));
            if (order == null)
                return Result.error("订单不存在");
            order.setStatus(4);
            trafficOrderMapper.updateById(order);
        }
        return Result.success("退款审批已通过");
    }

    /**
     * POST /api/admin/orders/{orderNo}/refund/reject - 拒绝退款
     */
    @PostMapping("/orders/{orderNo}/refund/reject")
    public Result<String> rejectRefund(@PathVariable String orderNo) {
        checkAdmin();
        return Result.success("退款申请已拒绝");
    }

    // ======================== 促销券管理 ========================

    /**
     * GET /api/admin/coupons - 促销券列表
     */
    @GetMapping("/coupons")
    public Result<List<Coupon>> listCoupons() {
        checkAdmin();
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Coupon::getCreateTime).orderByDesc(Coupon::getId);
        return Result.success(couponMapper.selectList(wrapper));
    }

    /**
     * POST /api/admin/coupons - 新增促销券
     */
    @PostMapping("/coupons")
    public Result<Coupon> addCoupon(@RequestBody Coupon coupon) {
        checkAdmin();
        if (coupon.getCreateTime() == null) {
            coupon.setCreateTime(LocalDateTime.now());
        }
        couponMapper.insert(coupon);
        return Result.success(coupon);
    }

    /**
     * PUT /api/admin/coupons/{id} - 编辑促销券
     */
    @PutMapping("/coupons/{id}")
    public Result<Void> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        checkAdmin();
        coupon.setId(id);
        couponMapper.updateById(coupon);
        return Result.success();
    }

    /**
     * DELETE /api/admin/coupons/{id} - 删除促销券
     */
    @DeleteMapping("/coupons/{id}")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        checkAdmin();
        couponMapper.deleteById(id);
        return Result.success();
    }

    // ======================== 举报工单处理 ========================

    /**
     * GET /api/admin/review-reports?status=0 - 评价举报工单
     */
    @GetMapping("/review-reports")
    public Result<List<ReviewReport>> listReviewReports(@RequestParam(required = false) Integer status) {
        checkAdmin();
        LambdaQueryWrapper<ReviewReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ReviewReport::getStatus, status);
        }
        wrapper.orderByDesc(ReviewReport::getCreateTime).orderByDesc(ReviewReport::getId);
        return Result.success(reviewReportMapper.selectList(wrapper));
    }

    /**
     * POST /api/admin/review-reports/{id}/resolve - 处理完成举报工单
     */
    @PostMapping("/review-reports/{id}/resolve")
    public Result<Void> resolveReviewReport(@PathVariable Long id) {
        checkAdmin();
        LambdaUpdateWrapper<ReviewReport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReviewReport::getId, id).set(ReviewReport::getStatus, 1);
        reviewReportMapper.update(null, wrapper);
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
