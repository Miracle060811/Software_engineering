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
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.HotelRoom;
import com.travelmate.entity.Post;
import com.travelmate.entity.ReviewReport;
import com.travelmate.entity.SysLog;
import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.entity.Train;
import com.travelmate.mapper.CouponMapper;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.mapper.ReviewReportMapper;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.HotelRoomStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private HotelOrderMapper hotelOrderMapper;

    @Autowired
    private HotelRoomStockService hotelRoomStockService;

    private static final int STATUS_REFUND_REJECTED = 5;

    private User checkAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new RuntimeException("无管理员权限");
        }
        return user;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + "不能为空");
        }
    }

    private void requireNonNegative(Number value, String fieldName) {
        if (value == null) {
            throw new RuntimeException(fieldName + "不能为空");
        }
        if (value instanceof BigDecimal decimal && decimal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(fieldName + "不能为负数");
        }
        if (!(value instanceof BigDecimal) && value.longValue() < 0) {
            throw new RuntimeException(fieldName + "不能为负数");
        }
    }

    private void requireTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new RuntimeException("出发/到达时间不能为空");
        }
        if (!end.isAfter(start)) {
            throw new RuntimeException("到达时间必须晚于出发时间");
        }
    }

    private void validateFlight(Flight flight) {
        requireText(flight.getFlightNo(), "航班号");
        requireText(flight.getAirline(), "航司");
        requireText(flight.getDepartureCity(), "出发城市");
        requireText(flight.getArrivalCity(), "到达城市");
        requireTimeRange(flight.getDepartureTime(), flight.getArrivalTime());
        requireNonNegative(flight.getEconomyPrice(), "经济舱价格");
        requireNonNegative(flight.getBusinessPrice(), "公务舱价格");
        requireNonNegative(flight.getTotalSeats(), "总座位数");
        requireNonNegative(flight.getAvailableSeats(), "可售座位数");
    }

    private void validateTrain(Train train) {
        requireText(train.getTrainNo(), "车次");
        requireText(train.getTrainType(), "车型");
        requireText(train.getDepartureStation(), "出发站");
        requireText(train.getArrivalStation(), "到达站");
        requireTimeRange(train.getDepartureTime(), train.getArrivalTime());
        requireNonNegative(train.getFirstClassPrice(), "一等座价格");
        requireNonNegative(train.getSecondClassPrice(), "二等座价格");
        requireNonNegative(train.getFirstClassSeats(), "一等座余票");
        requireNonNegative(train.getSecondClassSeats(), "二等座余票");
    }

    private void validateHotel(Hotel hotel) {
        requireText(hotel.getName(), "酒店名称");
        requireText(hotel.getCity(), "城市");
        requireText(hotel.getAddress(), "地址");
        requireNonNegative(hotel.getAvgPrice(), "均价");
        if (hotel.getStarRating() == null || hotel.getStarRating() < 1 || hotel.getStarRating() > 5) {
            throw new RuntimeException("星级必须在 1-5 之间");
        }
    }

    private void validateHotelRoom(HotelRoom room) {
        requireText(room.getRoomType(), "房型");
        requireText(room.getBedType(), "床型");
        requireNonNegative(room.getPrice(), "房型价格");
        requireNonNegative(room.getTotalRooms(), "总房量");
        requireNonNegative(room.getAvailableRooms(), "可售房量");
    }

    private long countTotalOrders() {
        return trafficOrderMapper.selectCount(null) + hotelOrderMapper.selectCount(null);
    }

    private long countTodayOrders() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return trafficOrderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>()
                .ge(TrafficOrder::getCreateTime, todayStart))
                + hotelOrderMapper.selectCount(new LambdaQueryWrapper<HotelOrder>()
                        .ge(HotelOrder::getCreateTime, todayStart));
    }

    private String buildTrafficRoute(TrafficOrder order) {
        if (Objects.equals(order.getOrderType(), 0)) {
            Flight flight = flightMapper.selectById(order.getTicketId());
            if (flight != null) {
                return flight.getDepartureCity() + "→" + flight.getArrivalCity();
            }
        }
        if (Objects.equals(order.getOrderType(), 1)) {
            Train train = trainMapper.selectById(order.getTicketId());
            if (train != null) {
                return train.getDepartureStation() + "→" + train.getArrivalStation();
            }
        }
        return "未知路线";
    }

    private List<LocalDate> lastSevenDays() {
        List<LocalDate> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            days.add(LocalDate.now().minusDays(i));
        }
        return days;
    }

    private List<Map<String, Object>> buildDailyOrderTrend() {
        Map<LocalDate, Long> trafficCounts = trafficOrderMapper.selectList(new LambdaQueryWrapper<TrafficOrder>()
                .ge(TrafficOrder::getCreateTime, LocalDate.now().minusDays(6).atStartOfDay()))
                .stream()
                .filter(o -> o.getCreateTime() != null)
                .collect(Collectors.groupingBy(o -> o.getCreateTime().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> hotelCounts = hotelOrderMapper.selectList(new LambdaQueryWrapper<HotelOrder>()
                .ge(HotelOrder::getCreateTime, LocalDate.now().minusDays(6).atStartOfDay()))
                .stream()
                .filter(o -> o.getCreateTime() != null)
                .collect(Collectors.groupingBy(o -> o.getCreateTime().toLocalDate(), Collectors.counting()));

        return lastSevenDays().stream().map(day -> {
            Map<String, Object> row = new HashMap<>();
            row.put("day", day.format(DateTimeFormatter.ofPattern("MM-dd")));
            row.put("count", trafficCounts.getOrDefault(day, 0L) + hotelCounts.getOrDefault(day, 0L));
            return row;
        }).toList();
    }

    private List<Map<String, Object>> buildUserGrowthTrend() {
        Map<LocalDate, Long> counts = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, LocalDate.now().minusDays(6).atStartOfDay()))
                .stream()
                .filter(u -> u.getCreateTime() != null)
                .collect(Collectors.groupingBy(u -> u.getCreateTime().toLocalDate(), Collectors.counting()));
        return lastSevenDays().stream().map(day -> {
            Map<String, Object> row = new HashMap<>();
            row.put("day", day.format(DateTimeFormatter.ofPattern("MM-dd")));
            row.put("count", counts.getOrDefault(day, 0L));
            return row;
        }).toList();
    }

    private List<Map<String, Object>> buildOrderTypeDist() {
        long flightCount = trafficOrderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderType, 0));
        long trainCount = trafficOrderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderType, 1));
        long hotelCount = hotelOrderMapper.selectCount(null);
        return List.of(
                Map.of("name", "机票", "value", flightCount),
                Map.of("name", "火车票", "value", trainCount),
                Map.of("name", "酒店", "value", hotelCount));
    }

    private List<Map<String, Object>> buildHotDestinations() {
        Map<String, Long> counts = new HashMap<>();
        trafficOrderMapper.selectList(null).forEach(order -> {
            String destination = null;
            if (Objects.equals(order.getOrderType(), 0)) {
                Flight flight = flightMapper.selectById(order.getTicketId());
                destination = flight == null ? null : flight.getArrivalCity();
            } else if (Objects.equals(order.getOrderType(), 1)) {
                Train train = trainMapper.selectById(order.getTicketId());
                destination = train == null ? null : train.getArrivalStation();
            }
            if (destination != null && !destination.isBlank()) {
                counts.merge(destination, 1L, Long::sum);
            }
        });
        hotelOrderMapper.selectList(null).forEach(order -> {
            Hotel hotel = hotelMapper.selectById(order.getHotelId());
            String city = hotel == null ? null : hotel.getCity();
            if (city != null && !city.isBlank()) {
                counts.merge(city, 1L, Long::sum);
            }
        });
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .toList();
    }

    private List<Map<String, Object>> buildLogTrend(boolean latency) {
        LocalDateTime start = LocalDateTime.now().minusMinutes(11).withSecond(0).withNano(0);
        Map<LocalDateTime, List<SysLog>> grouped = sysLogMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .ge(SysLog::getCreateTime, start))
                .stream()
                .filter(log -> log.getCreateTime() != null)
                .collect(Collectors.groupingBy(log -> log.getCreateTime().withSecond(0).withNano(0)));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDateTime minute = start.plusMinutes(i);
            List<SysLog> logs = grouped.getOrDefault(minute, List.of());
            long value;
            if (latency) {
                value = logs.stream()
                        .filter(log -> log.getTimeMs() != null)
                        .mapToLong(SysLog::getTimeMs)
                        .average()
                        .stream()
                        .mapToLong(Math::round)
                        .findFirst()
                        .orElse(0L);
            } else {
                value = logs.size();
            }
            trend.add(Map.of("time", minute.format(DateTimeFormatter.ofPattern("HH:mm")), "value", value));
        }
        return trend;
    }

    private List<Map<String, Object>> buildRecentErrorLogs() {
        return sysLogMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .eq(SysLog::getStatus, 0)
                .orderByDesc(SysLog::getCreateTime)
                .last("LIMIT 10"))
                .stream()
                .map(log -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("method", log.getMethod());
                    row.put("errorMsg", log.getErrorMsg());
                    row.put("timeMs", log.getTimeMs());
                    row.put("createTime", log.getCreateTime());
                    return row;
                }).toList();
    }

    private List<Map<String, Object>> buildAlerts(long pendingPosts) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long errorLogs = sysLogMapper.selectCount(new LambdaQueryWrapper<SysLog>()
                .eq(SysLog::getStatus, 0).ge(SysLog::getCreateTime, todayStart));
        if (errorLogs > 0) {
            alerts.add(Map.of("level", "danger", "title", "系统异常日志告警",
                    "message", "今日检测到 " + errorLogs + " 条失败操作日志，请尽快排查。"));
        }
        long lowStockRooms = hotelRoomMapper.selectCount(new LambdaQueryWrapper<HotelRoom>()
                .eq(HotelRoom::getStatus, 1).le(HotelRoom::getAvailableRooms, 2));
        if (lowStockRooms > 0) {
            alerts.add(Map.of("level", "warning", "title", "房态库存预警",
                    "message", "当前有 " + lowStockRooms + " 个房型库存低于等于 2。"));
        }
        if (pendingPosts > 0) {
            alerts.add(Map.of("level", "info", "title", "内容审核待处理",
                    "message", "待审核游记还有 " + pendingPosts + " 篇。"));
        }
        if (alerts.isEmpty()) {
            alerts.add(Map.of("level", "success", "title", "系统运行稳定",
                    "message", "当前未发现高优先级异常。"));
        }
        return alerts;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        checkAdmin();
        long pendingPosts = postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 0));
        long todayNewUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, LocalDate.now().atStartOfDay()));
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("totalOrders", countTotalOrders());
        stats.put("todayOrders", countTodayOrders());
        stats.put("pendingPosts", pendingPosts);
        stats.put("todayNewUsers", todayNewUsers);
        return Result.success(stats);
    }

    @GetMapping("/dashboard/data")
    public Result<Map<String, Object>> dashboardData() {
        checkAdmin();
        long pendingPosts = postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 0));
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalOrders", countTotalOrders());
        data.put("todayOrders", countTodayOrders());
        data.put("pendingPosts", pendingPosts);
        data.put("dailyTrend", buildDailyOrderTrend());
        data.put("hotDestinations", buildHotDestinations());
        data.put("orderTypeDist", buildOrderTypeDist());
        data.put("userGrowth", buildUserGrowthTrend());
        data.put("qpsTrend", buildLogTrend(false));
        data.put("latencyTrend", buildLogTrend(true));
        data.put("recentErrors", buildRecentErrorLogs());
        data.put("alerts", buildAlerts(pendingPosts));
        return Result.success(data);
    }

    @GetMapping("/flights")
    public Result<List<Flight>> listFlights() {
        checkAdmin();
        return Result.success(flightMapper.selectList(null));
    }

    @PostMapping("/flights")
    public Result<Flight> addFlight(@RequestBody Flight flight) {
        checkAdmin();
        validateFlight(flight);
        flightMapper.insert(flight);
        return Result.success(flight);
    }

    @PutMapping("/flights/{id}")
    public Result<Void> updateFlight(@PathVariable Long id, @RequestBody Flight flight) {
        checkAdmin();
        validateFlight(flight);
        flight.setId(id);
        flightMapper.updateById(flight);
        return Result.success();
    }

    @DeleteMapping("/flights/{id}")
    public Result<Void> deleteFlight(@PathVariable Long id) {
        checkAdmin();
        long orderCount = trafficOrderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderType, 0).eq(TrafficOrder::getTicketId, id));
        if (orderCount > 0) {
            return Result.error("该航班已有订单，不能删除");
        }
        flightMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/trains")
    public Result<List<Train>> listTrains() {
        checkAdmin();
        return Result.success(trainMapper.selectList(null));
    }

    @PostMapping("/trains")
    public Result<Train> addTrain(@RequestBody Train train) {
        checkAdmin();
        validateTrain(train);
        trainMapper.insert(train);
        return Result.success(train);
    }

    @PutMapping("/trains/{id}")
    public Result<Void> updateTrain(@PathVariable Long id, @RequestBody Train train) {
        checkAdmin();
        validateTrain(train);
        train.setId(id);
        trainMapper.updateById(train);
        return Result.success();
    }

    @DeleteMapping("/trains/{id}")
    public Result<Void> deleteTrain(@PathVariable Long id) {
        checkAdmin();
        long orderCount = trafficOrderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderType, 1).eq(TrafficOrder::getTicketId, id));
        if (orderCount > 0) {
            return Result.error("该车次已有订单，不能删除");
        }
        trainMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/hotels")
    public Result<List<Hotel>> listHotels() {
        checkAdmin();
        return Result.success(hotelMapper.selectList(null));
    }

    @PostMapping("/hotels")
    public Result<Hotel> addHotel(@RequestBody Hotel hotel) {
        checkAdmin();
        validateHotel(hotel);
        hotelMapper.insert(hotel);
        return Result.success(hotel);
    }

    @PutMapping("/hotels/{id}")
    public Result<Void> updateHotel(@PathVariable Long id, @RequestBody Hotel hotel) {
        checkAdmin();
        validateHotel(hotel);
        hotel.setId(id);
        hotelMapper.updateById(hotel);
        return Result.success();
    }

    @DeleteMapping("/hotels/{id}")
    public Result<Void> deleteHotel(@PathVariable Long id) {
        checkAdmin();
        long orderCount = hotelOrderMapper.selectCount(new LambdaQueryWrapper<HotelOrder>()
                .eq(HotelOrder::getHotelId, id));
        if (orderCount > 0) {
            return Result.error("该酒店已有订单，不能删除");
        }
        hotelMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    public Result<List<HotelRoom>> listHotelRooms(@PathVariable Long hotelId) {
        checkAdmin();
        return Result.success(hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                .eq(HotelRoom::getHotelId, hotelId).orderByAsc(HotelRoom::getId)));
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    public Result<HotelRoom> addHotelRoom(@PathVariable Long hotelId, @RequestBody HotelRoom room) {
        checkAdmin();
        validateHotelRoom(room);
        room.setHotelId(hotelId);
        hotelRoomMapper.insert(room);
        hotelRoomStockService.syncWithDatabase(room.getId());
        return Result.success(room);
    }

    @PutMapping("/hotel-rooms/{id}")
    public Result<Void> updateHotelRoom(@PathVariable Long id, @RequestBody HotelRoom room) {
        checkAdmin();
        validateHotelRoom(room);
        room.setId(id);
        hotelRoomMapper.updateById(room);
        hotelRoomStockService.syncWithDatabase(id);
        return Result.success();
    }

    @DeleteMapping("/hotel-rooms/{id}")
    public Result<Void> deleteHotelRoom(@PathVariable Long id) {
        checkAdmin();
        long orderCount = hotelOrderMapper.selectCount(new LambdaQueryWrapper<HotelOrder>()
                .eq(HotelOrder::getRoomId, id));
        if (orderCount > 0) {
            return Result.error("该房型已有订单，不能删除");
        }
        hotelRoomMapper.deleteById(id);
        hotelRoomStockService.syncWithDatabase(id);
        return Result.success();
    }

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

    @PostMapping("/posts/{id}/approve")
    public Result<Void> approvePost(@PathVariable Long id) {
        checkAdmin();
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getStatus, 1)
                .set(Post::getRejectReason, null));
        return Result.success();
    }

    @PostMapping("/posts/{id}/reject")
    public Result<Void> rejectPost(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        checkAdmin();
        String reason = body == null || body.get("reason") == null ? "内容不符合社区规范" : body.get("reason").toString();
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getStatus, 2)
                .set(Post::getRejectReason, reason));
        return Result.success();
    }

    @GetMapping("/users")
    public Result<List<User>> listUsers() {
        checkAdmin();
        return Result.success(userMapper.selectList(null));
    }

    @PostMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        checkAdmin();
        userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, id).set(User::getStatus, 0));
        return Result.success();
    }

    @PostMapping("/users/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        checkAdmin();
        userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, id).set(User::getStatus, 1));
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> listOrders(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdmin();
        List<Map<String, Object>> allOrders = new ArrayList<>();

        if (type == null || type.equals("all") || type.equals("flight") || type.equals("train")) {
            LambdaQueryWrapper<TrafficOrder> tw = new LambdaQueryWrapper<>();
            if ("flight".equals(type)) {
                tw.eq(TrafficOrder::getOrderType, 0);
            }
            if ("train".equals(type)) {
                tw.eq(TrafficOrder::getOrderType, 1);
            }
            if (status != null) {
                tw.eq(TrafficOrder::getStatus, status);
            }
            trafficOrderMapper.selectList(tw).forEach(o -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orderNo", o.getOrderNo());
                m.put("type", Objects.equals(o.getOrderType(), 0) ? "机票" : "火车票");
                m.put("route", buildTrafficRoute(o));
                m.put("passenger", o.getPassengerName());
                m.put("seatType", o.getSeatType());
                m.put("amount", o.getAmount());
                m.put("status", o.getStatus());
                m.put("createTime", o.getCreateTime());
                allOrders.add(m);
            });
        }

        if (type == null || type.equals("all") || type.equals("hotel")) {
            LambdaQueryWrapper<HotelOrder> hw = new LambdaQueryWrapper<>();
            if (status != null) {
                hw.eq(HotelOrder::getStatus, status);
            }
            hotelOrderMapper.selectList(hw).forEach(o -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orderNo", o.getOrderNo());
                m.put("type", "酒店");
                m.put("route", o.getHotelName() + " " + o.getRoomType());
                m.put("passenger", o.getGuestName());
                m.put("amount", o.getAmount());
                m.put("status", o.getStatus());
                m.put("createTime", o.getCreateTime());
                allOrders.add(m);
            });
        }

        allOrders.sort(Comparator.comparing(o -> String.valueOf(o.get("createTime")), Comparator.reverseOrder()));
        int total = allOrders.size();
        int start = Math.max(0, (page - 1) * size);
        int end = Math.min(start + size, total);
        Map<String, Object> result = new HashMap<>();
        result.put("records", start < total ? allOrders.subList(start, end) : List.of());
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @PostMapping("/orders/{orderNo}/refund/approve")
    public Result<String> approveRefund(@PathVariable String orderNo) {
        checkAdmin();
        if (orderNo.startsWith("HT")) {
            HotelOrder order = hotelOrderMapper.selectOne(new LambdaQueryWrapper<HotelOrder>()
                    .eq(HotelOrder::getOrderNo, orderNo));
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (!Objects.equals(order.getStatus(), 4)) {
                order.setStatus(4);
                hotelOrderMapper.updateById(order);
                hotelRoomMapper.returnRoom(order.getRoomId());
                hotelRoomStockService.syncWithDatabase(order.getRoomId());
            }
        } else {
            TrafficOrder order = trafficOrderMapper.selectOne(new LambdaQueryWrapper<TrafficOrder>()
                    .eq(TrafficOrder::getOrderNo, orderNo));
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (!Objects.equals(order.getStatus(), 4)) {
                order.setStatus(4);
                trafficOrderMapper.updateById(order);
                returnTrafficStock(order);
            }
        }
        return Result.success("退款审批已通过");
    }

    @PostMapping("/orders/{orderNo}/refund/reject")
    public Result<String> rejectRefund(@PathVariable String orderNo) {
        checkAdmin();
        if (orderNo.startsWith("HT")) {
            HotelOrder order = hotelOrderMapper.selectOne(new LambdaQueryWrapper<HotelOrder>()
                    .eq(HotelOrder::getOrderNo, orderNo));
            if (order == null) {
                return Result.error("订单不存在");
            }
            order.setStatus(STATUS_REFUND_REJECTED);
            hotelOrderMapper.updateById(order);
        } else {
            TrafficOrder order = trafficOrderMapper.selectOne(new LambdaQueryWrapper<TrafficOrder>()
                    .eq(TrafficOrder::getOrderNo, orderNo));
            if (order == null) {
                return Result.error("订单不存在");
            }
            order.setStatus(STATUS_REFUND_REJECTED);
            trafficOrderMapper.updateById(order);
        }
        return Result.success("退款申请已拒绝");
    }

    private void returnTrafficStock(TrafficOrder order) {
        if (order.getOrderType() == null) {
            return;
        }
        if (order.getOrderType() == 0) {
            flightMapper.returnSeat(order.getTicketId());
            return;
        }
        if (order.getOrderType() == 1) {
            if ("FirstClass".equalsIgnoreCase(order.getSeatType())) {
                trainMapper.returnFirstClassSeat(order.getTicketId());
            } else {
                trainMapper.returnSecondClassSeat(order.getTicketId());
            }
        }
    }

    @GetMapping("/coupons")
    public Result<List<Coupon>> listCoupons() {
        checkAdmin();
        return Result.success(couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .orderByDesc(Coupon::getCreateTime).orderByDesc(Coupon::getId)));
    }

    @PostMapping("/coupons")
    public Result<Coupon> addCoupon(@RequestBody Coupon coupon) {
        checkAdmin();
        coupon.setCategory(normalizeCouponCategory(coupon.getCategory()));
        if (coupon.getCreateTime() == null) {
            coupon.setCreateTime(LocalDateTime.now());
        }
        couponMapper.insert(coupon);
        return Result.success(coupon);
    }

    @PutMapping("/coupons/{id}")
    public Result<Void> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        checkAdmin();
        coupon.setId(id);
        coupon.setCategory(normalizeCouponCategory(coupon.getCategory()));
        couponMapper.updateById(coupon);
        return Result.success();
    }

    @DeleteMapping("/coupons/{id}")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        checkAdmin();
        couponMapper.deleteById(id);
        return Result.success();
    }

    private String normalizeCouponCategory(String category) {
        if (category == null || category.isBlank()) {
            return "all";
        }
        String value = category.trim().toLowerCase();
        if ("flight".equals(value) || "train".equals(value) || "hotel".equals(value)) {
            return value;
        }
        return "all";
    }

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

    @PostMapping("/review-reports/{id}/resolve")
    public Result<Void> resolveReviewReport(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        checkAdmin();
        String remark = body == null || body.get("remark") == null ? "已人工复核" : body.get("remark").toString();
        reviewReportMapper.update(null, new LambdaUpdateWrapper<ReviewReport>()
                .eq(ReviewReport::getId, id)
                .set(ReviewReport::getStatus, 1)
                .set(ReviewReport::getHandleRemark, remark)
                .set(ReviewReport::getHandleTime, LocalDateTime.now()));
        return Result.success();
    }

    @GetMapping("/sensitive-words")
    public Result<List<SysSensitiveWord>> listSensitiveWords() {
        checkAdmin();
        return Result.success(sensitiveWordMapper.selectList(null));
    }

    @PostMapping("/sensitive-words")
    public Result<SysSensitiveWord> addSensitiveWord(@RequestBody SysSensitiveWord word) {
        checkAdmin();
        requireText(word.getWord(), "敏感词");
        word.setCreateTime(LocalDateTime.now());
        sensitiveWordMapper.insert(word);
        return Result.success(word);
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        checkAdmin();
        sensitiveWordMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdmin();
        Page<SysLog> pageObj = new Page<>(page, size);
        Page<SysLog> result = sysLogMapper.selectPage(pageObj,
                new LambdaQueryWrapper<SysLog>().orderByDesc(SysLog::getCreateTime));
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }
}
