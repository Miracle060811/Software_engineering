package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.Comment;
import com.travelmate.entity.Coupon;
import com.travelmate.entity.Destination;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.HotelRoom;
import com.travelmate.entity.Post;
import com.travelmate.entity.Reply;
import com.travelmate.entity.Review;
import com.travelmate.entity.ReviewReport;
import com.travelmate.entity.SysLog;
import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.entity.Train;
import com.travelmate.entity.UserCoupon;
import com.travelmate.mapper.AttractionMapper;
import com.travelmate.mapper.CommentMapper;
import com.travelmate.mapper.CouponMapper;
import com.travelmate.mapper.DestinationMapper;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.mapper.ReplyMapper;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.mapper.ReviewReportMapper;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.mapper.UserCouponMapper;
import com.travelmate.service.HotelRoomStockService;
import com.travelmate.service.NotificationCenterService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import java.util.Set;
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
    private AttractionMapper attractionMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private SysSensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private DestinationMapper destinationMapper;

    @Autowired
    private ReviewReportMapper reviewReportMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private TrafficOrderMapper trafficOrderMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private HotelOrderMapper hotelOrderMapper;

    @Autowired
    private HotelRoomStockService hotelRoomStockService;

    @Autowired
    private NotificationCenterService notificationCenterService;

    private static final int STATUS_TRAFFIC_CANCELLED = 3;
    private static final int STATUS_TRAFFIC_REFUNDED = 4;
    private static final int STATUS_HOTEL_PAID = 1;
    private static final int STATUS_HOTEL_COMPLETED = 3;
    private static final int STATUS_HOTEL_CANCELLED_OR_REFUNDED = 4;
    private static final long MAX_CSV_FILE_SIZE = 5L * 1024L * 1024L;
    private static final int MAX_IMPORT_FAILURES_RETURNED = 50;
    private static final String CSV_MODE_UPSERT = "upsert";
    private static final Set<String> CSV_IMPORT_TYPES = Set.of(
            "flights", "trains", "hotels", "rooms", "attractions", "destinations");
    private static final Map<String, List<String>> CSV_REQUIRED_HEADERS = Map.of(
            "flights", List.of("flightNo", "airline", "departureCity", "arrivalCity", "departureTime", "arrivalTime",
                    "economyPrice", "businessPrice", "totalSeats", "availableSeats"),
            "trains", List.of("trainNo", "trainType", "departureStation", "arrivalStation", "departureTime",
                    "arrivalTime", "firstClassPrice", "secondClassPrice", "firstClassSeats", "secondClassSeats"),
            "hotels", List.of("name", "city", "address", "starRating", "avgPrice"),
            "rooms", List.of("hotelId", "roomType", "bedType", "price", "totalRooms", "availableRooms"),
            "attractions", List.of("name", "city", "address", "adultPrice", "childPrice", "totalTickets",
                    "availableTickets"),
            "destinations", List.of("slug", "name", "tag", "img", "desc", "intro"));

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
        if (flight.getAvailableSeats() > flight.getTotalSeats()) {
            throw new RuntimeException("可售座位数不能大于总座位数");
        }
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
        if (train.getDurationMinutes() == null) {
            train.setDurationMinutes((int) Duration.between(train.getDepartureTime(), train.getArrivalTime()).toMinutes());
        }
    }

    private void validateHotel(Hotel hotel) {
        requireText(hotel.getName(), "酒店名称");
        requireText(hotel.getCity(), "城市");
        requireText(hotel.getAddress(), "地址");
        requireNonNegative(hotel.getAvgPrice(), "均价");
        if (hotel.getStarRating() == null || hotel.getStarRating() < 1 || hotel.getStarRating() > 5) {
            throw new RuntimeException("星级必须在 1-5 之间");
        }
        if (hotel.getScore() != null
                && (hotel.getScore().compareTo(BigDecimal.ZERO) < 0 || hotel.getScore().compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new RuntimeException("评分必须在 0-5 之间");
        }
    }

    private void validateHotelRoom(HotelRoom room) {
        requireText(room.getRoomType(), "房型");
        requireText(room.getBedType(), "床型");
        requireNonNegative(room.getPrice(), "房型价格");
        requireNonNegative(room.getTotalRooms(), "总房量");
        requireNonNegative(room.getAvailableRooms(), "可售房量");
        if (room.getAvailableRooms() > room.getTotalRooms()) {
            throw new RuntimeException("可售房量不能大于总房量");
        }
    }

    private void validateAttraction(Attraction attraction) {
        requireText(attraction.getName(), "景点名称");
        requireText(attraction.getCity(), "城市");
        requireText(attraction.getAddress(), "地址");
        requireNonNegative(attraction.getAdultPrice(), "成人票价");
        requireNonNegative(attraction.getChildPrice(), "儿童票价");
        requireNonNegative(attraction.getTotalTickets(), "总票数");
        requireNonNegative(attraction.getAvailableTickets(), "可售票数");
        if (attraction.getAvailableTickets() > attraction.getTotalTickets()) {
            throw new RuntimeException("可售票数不能大于总票数");
        }
        if (attraction.getStatus() == null) {
            attraction.setStatus(1);
        }
    }

    private void validateCoupon(Coupon coupon) {
        requireText(coupon.getName(), "优惠券名称");
        if (coupon.getDiscountType() == null || (coupon.getDiscountType() != 0 && coupon.getDiscountType() != 1)) {
            throw new RuntimeException("优惠类型必须为满减或折扣");
        }
        requireNonNegative(coupon.getDiscountValue(), "优惠值");
        requireNonNegative(coupon.getMinAmount(), "最低消费");
        requireNonNegative(coupon.getStock(), "库存");
        if (coupon.getExpireDate() == null) {
            throw new RuntimeException("有效期不能为空");
        }
        if (coupon.getDiscountType() == 1 && coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("折扣比例必须大于 0");
        }
    }

    private void validateDestination(Destination destination) {
        requireText(destination.getSlug(), "城市标识");
        requireText(destination.getName(), "城市名称");
        requireText(destination.getCountry(), "国家/地区");
        requireText(destination.getTag(), "城市标签");
        requireText(destination.getImg(), "封面图");
        requireText(destination.getDesc(), "短描述");
        requireText(destination.getIntro(), "城市介绍");
        if (destination.getStatus() == null) {
            destination.setStatus(1);
        }
        if (destination.getSortOrder() == null) {
            destination.setSortOrder(100);
        }
    }

    private void validateSensitiveWordUnique(SysSensitiveWord word, Long excludeId) {
        requireText(word.getWord(), "敏感词");
        Long count = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SysSensitiveWord>()
                .eq(SysSensitiveWord::getWord, word.getWord().trim())
                .ne(excludeId != null, SysSensitiveWord::getId, excludeId));
        if (count > 0) {
            throw new RuntimeException("敏感词已存在");
        }
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

    private BigDecimal countTodayGmv() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        BigDecimal traffic = trafficOrderMapper.selectList(new LambdaQueryWrapper<TrafficOrder>()
                .ge(TrafficOrder::getCreateTime, todayStart)
                .in(TrafficOrder::getStatus, List.of(1, 2)))
                .stream()
                .map(TrafficOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal hotel = hotelOrderMapper.selectList(new LambdaQueryWrapper<HotelOrder>()
                .ge(HotelOrder::getCreateTime, todayStart)
                .in(HotelOrder::getStatus, List.of(1, 2, 3)))
                .stream()
                .map(HotelOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return traffic.add(hotel);
    }

    private long countOnlineUsers() {
        return sysLogMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .ge(SysLog::getCreateTime, LocalDateTime.now().minusMinutes(15))
                .isNotNull(SysLog::getUserId))
                .stream()
                .map(SysLog::getUserId)
                .distinct()
                .count();
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

    private Map<String, Object> buildPostAuditRow(Post post) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", post.getId());
        row.put("userId", post.getUserId());
        row.put("title", post.getTitle());
        row.put("content", post.getContent());
        row.put("destination", post.getDestination());
        row.put("tags", post.getTags());
        row.put("status", post.getStatus());
        row.put("rejectReason", post.getRejectReason());
        row.put("createTime", post.getCreateTime());
        User author = post.getUserId() == null ? null : userMapper.selectById(post.getUserId());
        row.put("authorUsername", author == null ? "未知用户" : author.getUsername());
        row.put("authorNickname", author == null ? null : author.getNickname());

        String joined = String.join(" ",
                Objects.toString(post.getTitle(), ""),
                Objects.toString(post.getContent(), ""),
                Objects.toString(post.getTags(), ""),
                Objects.toString(post.getDestination(), ""));
        List<String> matchedWords = sensitiveWordMapper.selectList(null).stream()
                .filter(word -> word.getWord() != null && !word.getWord().isBlank() && joined.contains(word.getWord()))
                .map(SysSensitiveWord::getWord)
                .distinct()
                .toList();
        row.put("matchedWords", matchedWords);
        if (Objects.equals(post.getStatus(), 0)) {
            row.put("aiSuggestion", matchedWords.isEmpty() ? "AI建议：未命中敏感词，可人工复核后通过" : "AI建议：命中敏感词，建议重点复核");
        } else if (Objects.equals(post.getStatus(), 2)) {
            row.put("aiSuggestion", post.getRejectReason() == null ? "已拒绝" : post.getRejectReason());
        } else {
            row.put("aiSuggestion", "已通过审核");
        }
        return row;
    }

    private Map<String, Object> buildReviewReportRow(ReviewReport report) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", report.getId());
        row.put("reviewId", report.getReviewId());
        row.put("reporterId", report.getReporterId());
        row.put("reason", report.getReason());
        row.put("status", report.getStatus());
        row.put("handleRemark", report.getHandleRemark());
        row.put("createTime", report.getCreateTime());
        row.put("handleTime", report.getHandleTime());

        User reporter = report.getReporterId() == null ? null : userMapper.selectById(report.getReporterId());
        row.put("reporterUsername", reporter == null ? "未知用户" : reporter.getUsername());
        Review review = report.getReviewId() == null ? null : reviewMapper.selectById(report.getReviewId());
        if (review != null) {
            row.put("reviewContent", review.getContent());
            row.put("rating", review.getRating());
            row.put("targetType", review.getTargetType());
            row.put("reviewUserId", review.getUserId());
            User reviewUser = review.getUserId() == null ? null : userMapper.selectById(review.getUserId());
            row.put("reviewUsername", reviewUser == null ? "未知用户" : reviewUser.getUsername());
            if (Objects.equals(review.getTargetType(), 0)) {
                Hotel hotel = hotelMapper.selectById(review.getTargetId());
                row.put("targetName", hotel == null ? "未知酒店" : hotel.getName());
            } else {
                Attraction attraction = attractionMapper.selectById(review.getTargetId());
                row.put("targetName", attraction == null ? "未知景点" : attraction.getName());
            }
        }
        return row;
    }

    private Map<String, Object> buildUserProfileRow(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("nickname", user.getNickname());
        row.put("email", user.getEmail());
        row.put("phone", user.getPhone());
        row.put("bio", user.getBio());
        row.put("role", user.getRole());
        row.put("level", user.getLevel());
        row.put("status", user.getStatus());
        row.put("createTime", user.getCreateTime());
        row.put("updateTime", user.getUpdateTime());
        row.put("trafficOrderCount", trafficOrderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>().eq(TrafficOrder::getUserId, user.getId())));
        row.put("hotelOrderCount", hotelOrderMapper.selectCount(new LambdaQueryWrapper<HotelOrder>().eq(HotelOrder::getUserId, user.getId())));
        row.put("postCount", postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getUserId, user.getId())));
        row.put("commentCount", commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, user.getId())));
        row.put("reviewCount", reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getUserId, user.getId()).eq(Review::getDeleted, 0)));
        row.put("reportCount", reviewReportMapper.selectCount(new LambdaQueryWrapper<ReviewReport>().eq(ReviewReport::getReporterId, user.getId())));
        SysLog lastLog = sysLogMapper.selectOne(new LambdaQueryWrapper<SysLog>()
                .eq(SysLog::getUserId, user.getId())
                .orderByDesc(SysLog::getCreateTime)
                .last("LIMIT 1"));
        row.put("lastOperationTime", lastLog == null ? null : lastLog.getCreateTime());
        row.put("lastOperation", lastLog == null ? null : lastLog.getOperation());
        return row;
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
        stats.put("todayGmv", countTodayGmv());
        stats.put("onlineUsers", countOnlineUsers());
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
        data.put("todayGmv", countTodayGmv());
        data.put("onlineUsers", countOnlineUsers());
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

    @GetMapping("/attractions")
    public Result<List<Attraction>> listAttractions() {
        checkAdmin();
        return Result.success(attractionMapper.selectList(new LambdaQueryWrapper<Attraction>()
                .orderByAsc(Attraction::getCity).orderByDesc(Attraction::getId)));
    }

    @PostMapping("/attractions")
    public Result<Attraction> addAttraction(@RequestBody Attraction attraction) {
        checkAdmin();
        validateAttraction(attraction);
        attractionMapper.insert(attraction);
        return Result.success(attraction);
    }

    @PutMapping("/attractions/{id}")
    public Result<Void> updateAttraction(@PathVariable Long id, @RequestBody Attraction attraction) {
        checkAdmin();
        validateAttraction(attraction);
        attraction.setId(id);
        attractionMapper.updateById(attraction);
        return Result.success();
    }

    @DeleteMapping("/attractions/{id}")
    public Result<Void> deleteAttraction(@PathVariable Long id) {
        checkAdmin();
        long reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getTargetType, 1)
                .eq(Review::getTargetId, id)
                .eq(Review::getDeleted, 0));
        if (reviewCount > 0) {
            attractionMapper.update(null, new LambdaUpdateWrapper<Attraction>()
                    .eq(Attraction::getId, id)
                    .set(Attraction::getStatus, 0));
            return Result.success();
        }
        attractionMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/posts")
    public Result<List<Map<String, Object>>> listPosts(@RequestParam(required = false) Integer status) {
        checkAdmin();
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Post::getStatus, status);
        }
        wrapper.orderByDesc(Post::getCreateTime);
        return Result.success(postMapper.selectList(wrapper).stream()
                .map(this::buildPostAuditRow)
                .toList());
    }

    @GetMapping("/destinations")
    public Result<List<Destination>> listAdminDestinations() {
        checkAdmin();
        return Result.success(destinationMapper.selectList(new LambdaQueryWrapper<Destination>()
                .orderByAsc(Destination::getSortOrder)
                .orderByDesc(Destination::getId)));
    }

    @PutMapping("/destinations/{id}")
    public Result<Void> updateDestination(@PathVariable Long id, @RequestBody Destination destination) {
        checkAdmin();
        destination.setId(id);
        validateDestination(destination);
        destination.setUpdateTime(LocalDateTime.now());
        destinationMapper.updateById(destination);
        return Result.success();
    }

    @DeleteMapping("/destinations/{id}")
    public Result<Void> deleteDestination(@PathVariable Long id) {
        checkAdmin();
        destinationMapper.update(null, new LambdaUpdateWrapper<Destination>()
                .eq(Destination::getId, id)
                .set(Destination::getStatus, 0)
                .set(Destination::getUpdateTime, LocalDateTime.now()));
        return Result.success();
    }

    @PostMapping("/posts/{id}/approve")
    public Result<Void> approvePost(@PathVariable Long id) {
        checkAdmin();
        Post post = postMapper.selectById(id);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getStatus, 1)
                .set(Post::getRejectReason, null)
                .set(Post::getUpdateTime, LocalDateTime.now()));
        if (post != null) {
            notificationCenterService.createNotification(
                    post.getUserId(),
                    "post_audit",
                    "游记审核通过",
                    String.format("《%s》已通过人工审核并发布。", post.getTitle()),
                    "/post/" + post.getId());
        }
        return Result.success();
    }

    @PostMapping("/posts/{id}/reject")
    public Result<Void> rejectPost(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        checkAdmin();
        String reason = body == null || body.get("reason") == null ? "内容不符合社区规范" : body.get("reason").toString();
        Post post = postMapper.selectById(id);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getStatus, 2)
                .set(Post::getRejectReason, reason)
                .set(Post::getUpdateTime, LocalDateTime.now()));
        if (post != null) {
            notificationCenterService.createNotification(
                    post.getUserId(),
                    "post_audit",
                    "游记审核未通过",
                    String.format("《%s》未通过人工审核，原因：%s", post.getTitle(), reason),
                    "/post/" + post.getId());
        }
        return Result.success();
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> listUsers() {
        checkAdmin();
        return Result.success(userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime))
                .stream()
                .map(this::buildUserProfileRow)
                .toList());
    }

    @PostMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        User admin = checkAdmin();
        if (Objects.equals(admin.getId(), id)) {
            throw new RuntimeException("不能禁用当前管理员账号");
        }
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
                m.put("route", o.getHotelName() + " " + o.getRoomType() + " x" + (o.getRoomCount() == null ? 1 : o.getRoomCount()));
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
            if (Objects.equals(order.getStatus(), STATUS_HOTEL_CANCELLED_OR_REFUNDED)) {
                return Result.success("订单已取消或已退款，无需重复处理");
            }
            if (Objects.equals(order.getStatus(), STATUS_HOTEL_COMPLETED)) {
                return Result.error("已完成的酒店订单不能直接退款");
            }
            if (!Objects.equals(order.getStatus(), STATUS_HOTEL_PAID)) {
                return Result.error("只有已支付且未入住的酒店订单可以退款");
            }
            order.setStatus(STATUS_HOTEL_CANCELLED_OR_REFUNDED);
            hotelOrderMapper.updateById(order);
            hotelRoomMapper.returnRoom(order.getRoomId(), order.getRoomCount() == null ? 1 : order.getRoomCount());
            hotelRoomStockService.syncWithDatabase(order.getRoomId());
        } else {
            TrafficOrder order = trafficOrderMapper.selectOne(new LambdaQueryWrapper<TrafficOrder>()
                    .eq(TrafficOrder::getOrderNo, orderNo));
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (Objects.equals(order.getStatus(), STATUS_TRAFFIC_REFUNDED)) {
                return Result.success("订单已退票，无需重复处理");
            }
            if (Objects.equals(order.getStatus(), STATUS_TRAFFIC_CANCELLED)) {
                return Result.error("已取消的待支付订单库存已归还，不能再次退款");
            }
            if (!Objects.equals(order.getStatus(), 1) && !Objects.equals(order.getStatus(), 2)) {
                return Result.error("只有出票中或已出票的订单可以退票");
            }
            order.setStatus(STATUS_TRAFFIC_REFUNDED);
            trafficOrderMapper.updateById(order);
            returnTrafficStock(order);
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
            return Result.error("当前没有独立的退款申请状态，不能将酒店订单标记为拒绝退款");
        } else {
            TrafficOrder order = trafficOrderMapper.selectOne(new LambdaQueryWrapper<TrafficOrder>()
                    .eq(TrafficOrder::getOrderNo, orderNo));
            if (order == null) {
                return Result.error("订单不存在");
            }
            return Result.error("当前没有独立的退款申请状态，不能将票务订单标记为拒绝退款");
        }
    }

    private void returnTrafficStock(TrafficOrder order) {
        if (order.getOrderType() == null) {
            return;
        }
        if (order.getOrderType() == 0) {
            flightMapper.returnSeat(order.getTicketId(), getTicketCount(order));
            return;
        }
        if (order.getOrderType() == 1) {
            if ("FirstClass".equalsIgnoreCase(order.getSeatType())) {
                trainMapper.returnFirstClassSeat(order.getTicketId(), getTicketCount(order));
            } else {
                trainMapper.returnSecondClassSeat(order.getTicketId(), getTicketCount(order));
            }
        }
    }

    private int getTicketCount(TrafficOrder order) {
        return order.getTicketCount() == null ? 1 : order.getTicketCount();
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
        validateCoupon(coupon);
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
        validateCoupon(coupon);
        couponMapper.updateById(coupon);
        return Result.success();
    }

    @DeleteMapping("/coupons/{id}")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        checkAdmin();
        long claimedCount = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, id));
        if (claimedCount > 0) {
            couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                    .eq(Coupon::getId, id)
                    .set(Coupon::getStatus, 1)
                    .set(Coupon::getStock, 0));
            return Result.success();
        }
        couponMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/coupons/{id}/claims")
    public Result<List<Map<String, Object>>> listCouponClaims(@PathVariable Long id) {
        checkAdmin();
        List<UserCoupon> claims = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, id)
                .orderByDesc(UserCoupon::getReceivedTime));
        return Result.success(claims.stream().map(claim -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", claim.getId());
            row.put("userId", claim.getUserId());
            User user = userMapper.selectById(claim.getUserId());
            row.put("username", user == null ? "未知用户" : user.getUsername());
            row.put("nickname", user == null ? null : user.getNickname());
            row.put("status", claim.getStatus());
            row.put("receivedTime", claim.getReceivedTime());
            row.put("usedTime", claim.getUsedTime());
            return row;
        }).toList());
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
    public Result<List<Map<String, Object>>> listReviewReports(@RequestParam(required = false) Integer status) {
        checkAdmin();
        LambdaQueryWrapper<ReviewReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ReviewReport::getStatus, status);
        }
        wrapper.orderByDesc(ReviewReport::getCreateTime).orderByDesc(ReviewReport::getId);
        return Result.success(reviewReportMapper.selectList(wrapper).stream()
                .map(this::buildReviewReportRow)
                .toList());
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

    @PostMapping("/review-reports/{id}/reject")
    public Result<Void> rejectReviewReport(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        checkAdmin();
        String remark = body == null || body.get("remark") == null ? "举报不成立，已驳回" : body.get("remark").toString();
        reviewReportMapper.update(null, new LambdaUpdateWrapper<ReviewReport>()
                .eq(ReviewReport::getId, id)
                .set(ReviewReport::getStatus, 1)
                .set(ReviewReport::getHandleRemark, remark)
                .set(ReviewReport::getHandleTime, LocalDateTime.now()));
        return Result.success();
    }

    @PostMapping("/review-reports/{id}/delete-review")
    public Result<Void> deleteReportedReview(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        checkAdmin();
        ReviewReport report = reviewReportMapper.selectById(id);
        if (report == null) {
            return Result.error("举报工单不存在");
        }
        Review review = reviewMapper.selectById(report.getReviewId());
        if (review == null) {
            return Result.error("评价不存在");
        }
        reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                .eq(Review::getId, review.getId())
                .set(Review::getDeleted, 1));
        String remark = body == null || body.get("remark") == null ? "举报成立，评价已删除" : body.get("remark").toString();
        reviewReportMapper.update(null, new LambdaUpdateWrapper<ReviewReport>()
                .eq(ReviewReport::getId, id)
                .set(ReviewReport::getStatus, 1)
                .set(ReviewReport::getHandleRemark, remark)
                .set(ReviewReport::getHandleTime, LocalDateTime.now()));
        return Result.success();
    }

    @GetMapping("/reviews/{reviewId}/replies")
    public Result<List<Reply>> listReviewReplies(@PathVariable Long reviewId) {
        checkAdmin();
        return Result.success(replyMapper.selectList(new LambdaQueryWrapper<Reply>()
                .eq(Reply::getReviewId, reviewId)
                .eq(Reply::getDeleted, 0)
                .orderByAsc(Reply::getCreateTime)));
    }

    @PostMapping("/reviews/{reviewId}/replies")
    public Result<Reply> addReviewReply(@PathVariable Long reviewId, @RequestBody Map<String, Object> body) {
        User admin = checkAdmin();
        String content = body == null || body.get("content") == null ? "" : body.get("content").toString();
        requireText(content, "回复内容");
        Reply reply = new Reply();
        reply.setReviewId(reviewId);
        reply.setUserId(admin.getId());
        reply.setContent(content.trim());
        reply.setDeleted(0);
        reply.setCreateTime(LocalDateTime.now());
        replyMapper.insert(reply);
        return Result.success(reply);
    }

    @DeleteMapping("/replies/{id}")
    public Result<Void> deleteReply(@PathVariable Long id) {
        checkAdmin();
        replyMapper.update(null, new LambdaUpdateWrapper<Reply>()
                .eq(Reply::getId, id)
                .set(Reply::getDeleted, 1));
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
        validateSensitiveWordUnique(word, null);
        word.setWord(word.getWord().trim());
        word.setCreateTime(LocalDateTime.now());
        sensitiveWordMapper.insert(word);
        return Result.success(word);
    }

    @PutMapping("/sensitive-words/{id}")
    public Result<Void> updateSensitiveWord(@PathVariable Long id, @RequestBody SysSensitiveWord word) {
        checkAdmin();
        validateSensitiveWordUnique(word, id);
        word.setId(id);
        word.setWord(word.getWord().trim());
        sensitiveWordMapper.updateById(word);
        return Result.success();
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        checkAdmin();
        sensitiveWordMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/import/{type}/template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable String type) {
        checkAdmin();
        String normalizedType = normalizeCsvType(type);
        String csv = "\uFEFF" + String.join(",", csvHeadersFor(normalizedType)) + "\n"
                + csvTemplateSample(normalizedType) + "\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"travelmate-" + normalizedType + "-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/import/{type}")
    public Result<Map<String, Object>> importCsv(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestParam(defaultValue = "insert") String mode) {
        checkAdmin();
        if (file == null || file.isEmpty()) {
            return Result.error("CSV 文件不能为空");
        }
        if (file.getSize() > MAX_CSV_FILE_SIZE) {
            return Result.error("CSV 文件不能超过 5MB");
        }
        try {
            String normalizedType = normalizeCsvType(type);
            boolean upsert = CSV_MODE_UPSERT.equalsIgnoreCase(mode);
            int success = 0;
            int failed = 0;
            int total = 0;
            Map<String, Integer> actionCounts = new LinkedHashMap<>();
            List<Map<String, Object>> failures = new ArrayList<>();

            try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                    CSVParser parser = CSVFormat.DEFAULT.builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .setIgnoreEmptyLines(true)
                            .setTrim(true)
                            .build()
                            .parse(reader)) {
                List<String> headers = new ArrayList<>(parser.getHeaderMap().keySet());
                List<String> missingHeaders = findMissingHeaders(normalizedType, headers);
                if (!missingHeaders.isEmpty()) {
                    return Result.error("CSV 缺少必填表头：" + String.join(", ", missingHeaders));
                }

                for (CSVRecord record : parser) {
                    Map<String, String> row = mapCsvRecord(headers, record);
                    if (isBlankCsvRow(row)) {
                        continue;
                    }
                    total++;
                    try {
                        String action = importCsvRow(normalizedType, row, dryRun, upsert);
                        success++;
                        actionCounts.merge(action, 1, Integer::sum);
                    } catch (Exception e) {
                        failed++;
                        if (failures.size() < MAX_IMPORT_FAILURES_RETURNED) {
                            failures.add(Map.of(
                                    "line", record.getRecordNumber() + 1,
                                    "reason", e.getMessage()));
                        }
                    }
                }
            }
            if (total == 0) {
                return Result.error("CSV 至少需要一行有效数据");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", normalizedType);
            result.put("mode", upsert ? CSV_MODE_UPSERT : "insert");
            result.put("dryRun", dryRun);
            result.put("total", total);
            result.put("success", success);
            result.put("failed", failed);
            result.put("inserted", actionCounts.getOrDefault("inserted", 0));
            result.put("updated", actionCounts.getOrDefault("updated", 0));
            result.put("validated", actionCounts.getOrDefault("validated", 0));
            result.put("failures", failures);
            result.put("failureLimit", MAX_IMPORT_FAILURES_RETURNED);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    private String normalizeCsvType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        if (!CSV_IMPORT_TYPES.contains(normalized)) {
            throw new RuntimeException("不支持的导入类型：" + type);
        }
        return normalized;
    }

    private List<String> findMissingHeaders(String type, List<String> headers) {
        Set<String> normalizedHeaders = headers.stream()
                .map(this::normalizeHeaderName)
                .collect(Collectors.toSet());
        return CSV_REQUIRED_HEADERS.getOrDefault(type, List.of()).stream()
                .filter(required -> !normalizedHeaders.contains(normalizeHeaderName(required)))
                .toList();
    }

    private Map<String, String> mapCsvRecord(List<String> headers, CSVRecord record) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = cleanCsvHeader(headers.get(i));
            String value = i < record.size() ? record.get(i).trim() : "";
            row.put(header, value);
            row.put(normalizeHeaderName(header), value);
        }
        return row;
    }

    private boolean isBlankCsvRow(Map<String, String> row) {
        return row.values().stream().allMatch(value -> value == null || value.isBlank());
    }

    private String importCsvRow(String type, Map<String, String> row, boolean dryRun, boolean upsert) {
        switch (type) {
            case "flights" -> {
                Flight flight = new Flight();
                flight.setFlightNo(csv(row, "flightNo"));
                flight.setAirline(csv(row, "airline"));
                flight.setDepartureCity(csv(row, "departureCity"));
                flight.setArrivalCity(csv(row, "arrivalCity"));
                flight.setDepartureTime(dateTime(row, "departureTime"));
                flight.setArrivalTime(dateTime(row, "arrivalTime"));
                flight.setEconomyPrice(decimal(row, "economyPrice"));
                flight.setBusinessPrice(decimal(row, "businessPrice"));
                flight.setTotalSeats(integer(row, "totalSeats"));
                flight.setAvailableSeats(integer(row, "availableSeats"));
                flight.setStatus(optionalInteger(row, "status", 1));
                validateFlight(flight);
                if (dryRun) {
                    return "validated";
                }
                if (upsert) {
                    Flight exists = flightMapper.selectOne(new LambdaQueryWrapper<Flight>()
                            .eq(Flight::getFlightNo, flight.getFlightNo())
                            .last("LIMIT 1"));
                    if (exists != null) {
                        flight.setId(exists.getId());
                        flightMapper.updateById(flight);
                        return "updated";
                    }
                }
                flightMapper.insert(flight);
                return "inserted";
            }
            case "trains" -> {
                Train train = new Train();
                train.setTrainNo(csv(row, "trainNo"));
                train.setTrainType(csv(row, "trainType"));
                train.setDepartureStation(csv(row, "departureStation"));
                train.setArrivalStation(csv(row, "arrivalStation"));
                train.setDepartureTime(dateTime(row, "departureTime"));
                train.setArrivalTime(dateTime(row, "arrivalTime"));
                train.setDurationMinutes(optionalInteger(row, "durationMinutes", null));
                train.setFirstClassPrice(decimal(row, "firstClassPrice"));
                train.setSecondClassPrice(decimal(row, "secondClassPrice"));
                train.setFirstClassSeats(integer(row, "firstClassSeats"));
                train.setSecondClassSeats(integer(row, "secondClassSeats"));
                train.setStatus(optionalInteger(row, "status", 1));
                validateTrain(train);
                if (dryRun) {
                    return "validated";
                }
                if (upsert) {
                    Train exists = trainMapper.selectOne(new LambdaQueryWrapper<Train>()
                            .eq(Train::getTrainNo, train.getTrainNo())
                            .last("LIMIT 1"));
                    if (exists != null) {
                        train.setId(exists.getId());
                        trainMapper.updateById(train);
                        return "updated";
                    }
                }
                trainMapper.insert(train);
                return "inserted";
            }
            case "hotels" -> {
                Hotel hotel = new Hotel();
                hotel.setName(csv(row, "name"));
                hotel.setCity(csv(row, "city"));
                hotel.setAddress(csv(row, "address"));
                hotel.setDescription(optionalText(row, "description", ""));
                hotel.setCoverImg(optionalText(row, "coverImg", ""));
                hotel.setLat(optionalDecimal(row, "lat"));
                hotel.setLng(optionalDecimal(row, "lng"));
                hotel.setStarRating(integer(row, "starRating"));
                hotel.setAvgPrice(decimal(row, "avgPrice"));
                hotel.setScore(optionalDecimal(row, "score", BigDecimal.valueOf(4.5)));
                hotel.setStatus(optionalInteger(row, "status", 1));
                validateHotel(hotel);
                if (dryRun) {
                    return "validated";
                }
                if (upsert) {
                    Hotel exists = hotelMapper.selectOne(new LambdaQueryWrapper<Hotel>()
                            .eq(Hotel::getName, hotel.getName())
                            .eq(Hotel::getCity, hotel.getCity())
                            .eq(Hotel::getAddress, hotel.getAddress())
                            .last("LIMIT 1"));
                    if (exists != null) {
                        hotel.setId(exists.getId());
                        hotelMapper.updateById(hotel);
                        return "updated";
                    }
                }
                hotelMapper.insert(hotel);
                return "inserted";
            }
            case "rooms" -> {
                HotelRoom room = new HotelRoom();
                room.setHotelId(longValue(row, "hotelId"));
                if (hotelMapper.selectById(room.getHotelId()) == null) {
                    throw new RuntimeException("酒店ID不存在：" + room.getHotelId());
                }
                room.setRoomType(csv(row, "roomType"));
                room.setBedType(csv(row, "bedType"));
                room.setArea(optionalInteger(row, "area", 30));
                room.setPrice(decimal(row, "price"));
                room.setTotalRooms(integer(row, "totalRooms"));
                room.setAvailableRooms(integer(row, "availableRooms"));
                room.setImages(optionalText(row, "images", ""));
                room.setFacilities(optionalText(row, "facilities", ""));
                room.setStatus(optionalInteger(row, "status", 1));
                validateHotelRoom(room);
                if (dryRun) {
                    return "validated";
                }
                if (upsert) {
                    HotelRoom exists = hotelRoomMapper.selectOne(new LambdaQueryWrapper<HotelRoom>()
                            .eq(HotelRoom::getHotelId, room.getHotelId())
                            .eq(HotelRoom::getRoomType, room.getRoomType())
                            .last("LIMIT 1"));
                    if (exists != null) {
                        room.setId(exists.getId());
                        hotelRoomMapper.updateById(room);
                        hotelRoomStockService.syncWithDatabase(room.getId());
                        return "updated";
                    }
                }
                hotelRoomMapper.insert(room);
                hotelRoomStockService.syncWithDatabase(room.getId());
                return "inserted";
            }
            case "attractions" -> {
                Attraction attraction = new Attraction();
                attraction.setName(csv(row, "name"));
                attraction.setCity(csv(row, "city"));
                attraction.setAddress(csv(row, "address"));
                attraction.setDescription(optionalText(row, "description", ""));
                attraction.setCoverImg(optionalText(row, "coverImg", ""));
                attraction.setAdultPrice(decimal(row, "adultPrice"));
                attraction.setChildPrice(decimal(row, "childPrice"));
                attraction.setTotalTickets(integer(row, "totalTickets"));
                attraction.setAvailableTickets(integer(row, "availableTickets"));
                attraction.setOpenTime(optionalText(row, "openTime", ""));
                attraction.setLat(optionalDecimal(row, "lat"));
                attraction.setLng(optionalDecimal(row, "lng"));
                attraction.setOfficialUrl(optionalText(row, "officialUrl", ""));
                attraction.setSourceName(optionalText(row, "sourceName", ""));
                attraction.setDataCheckedDate(optionalDate(row, "dataCheckedDate"));
                attraction.setStatus(optionalInteger(row, "status", 1));
                validateAttraction(attraction);
                if (dryRun) {
                    return "validated";
                }
                if (upsert) {
                    Attraction exists = attractionMapper.selectOne(new LambdaQueryWrapper<Attraction>()
                            .eq(Attraction::getName, attraction.getName())
                            .eq(Attraction::getCity, attraction.getCity())
                            .last("LIMIT 1"));
                    if (exists != null) {
                        attraction.setId(exists.getId());
                        attractionMapper.updateById(attraction);
                        return "updated";
                    }
                }
                attractionMapper.insert(attraction);
                return "inserted";
            }
            case "destinations" -> {
                Destination destination = new Destination();
                destination.setSlug(csv(row, "slug"));
                destination.setName(csv(row, "name"));
                destination.setCountry(optionalText(row, "country", "中国"));
                destination.setTag(csv(row, "tag"));
                destination.setKeywords(optionalText(row, "keywords", ""));
                destination.setImg(csv(row, "img"));
                destination.setDesc(csv(row, "desc"));
                destination.setIntro(csv(row, "intro"));
                destination.setHighlights(optionalText(row, "highlights", ""));
                destination.setCulture(optionalText(row, "culture", ""));
                destination.setBestSeason(optionalText(row, "bestSeason", ""));
                destination.setTransport(optionalText(row, "transport", ""));
                destination.setSourceName(optionalText(row, "sourceName", ""));
                destination.setSourceUrl(optionalText(row, "sourceUrl", ""));
                destination.setSortOrder(optionalInteger(row, "sortOrder", 100));
                destination.setStatus(optionalInteger(row, "status", 1));
                validateDestination(destination);
                if (dryRun) {
                    return "validated";
                }
                Destination exists = destinationMapper.selectOne(new LambdaQueryWrapper<Destination>()
                        .eq(Destination::getSlug, destination.getSlug())
                        .last("LIMIT 1"));
                destination.setUpdateTime(LocalDateTime.now());
                if (exists == null) {
                    destination.setCreateTime(LocalDateTime.now());
                    destinationMapper.insert(destination);
                } else {
                    destination.setId(exists.getId());
                    destination.setCreateTime(exists.getCreateTime());
                    destinationMapper.updateById(destination);
                    return "updated";
                }
                return "inserted";
            }
            default -> throw new RuntimeException("不支持的导入类型：" + type);
        }
    }

    private String cleanCsvHeader(String header) {
        return header == null ? "" : header.replace("\uFEFF", "").trim();
    }

    private String normalizeHeaderName(String header) {
        return cleanCsvHeader(header)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase();
    }

    private String csv(Map<String, String> row, String key) {
        String value = row.get(key);
        if ((value == null || value.isBlank())) {
            value = row.get(normalizeHeaderName(key));
        }
        if (value == null || value.isBlank()) {
            throw new RuntimeException("缺少字段 " + key);
        }
        return value.trim();
    }

    private String optionalText(Map<String, String> row, String key, String fallback) {
        String value = row.get(key);
        if (value == null) {
            value = row.get(normalizeHeaderName(key));
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Integer integer(Map<String, String> row, String key) {
        try {
            return Integer.valueOf(normalizeNumber(csv(row, key)));
        } catch (Exception e) {
            throw new RuntimeException("字段 " + key + " 必须是整数");
        }
    }

    private Integer optionalInteger(Map<String, String> row, String key, Integer fallback) {
        String value = optionalText(row, key, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.valueOf(normalizeNumber(value));
        } catch (Exception e) {
            throw new RuntimeException("字段 " + key + " 必须是整数");
        }
    }

    private Long longValue(Map<String, String> row, String key) {
        try {
            return Long.valueOf(normalizeNumber(csv(row, key)));
        } catch (Exception e) {
            throw new RuntimeException("字段 " + key + " 必须是整数");
        }
    }

    private BigDecimal decimal(Map<String, String> row, String key) {
        try {
            return new BigDecimal(normalizeNumber(csv(row, key)));
        } catch (Exception e) {
            throw new RuntimeException("字段 " + key + " 必须是数字");
        }
    }

    private BigDecimal optionalDecimal(Map<String, String> row, String key) {
        String value = optionalText(row, key, "");
        if (value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(normalizeNumber(value));
        } catch (Exception e) {
            throw new RuntimeException("字段 " + key + " 必须是数字");
        }
    }

    private BigDecimal optionalDecimal(Map<String, String> row, String key, BigDecimal fallback) {
        BigDecimal value = optionalDecimal(row, key);
        return value == null ? fallback : value;
    }

    private String normalizeNumber(String value) {
        return value.trim().replace("￥", "").replace("¥", "").replace(",", "");
    }

    private LocalDateTime dateTime(Map<String, String> row, String key) {
        String value = csv(row, key);
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("字段 " + key + " 时间格式应为 2026-06-01T08:00:00 或 2026-06-01 08:00:00");
    }

    private LocalDate optionalDate(Map<String, String> row, String key) {
        String value = optionalText(row, key, "");
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new RuntimeException("字段 " + key + " 日期格式应为 2026-05-30");
        }
    }

    private List<String> csvHeadersFor(String type) {
        return switch (type) {
            case "flights" -> List.of("flightNo", "airline", "departureCity", "arrivalCity", "departureTime",
                    "arrivalTime", "economyPrice", "businessPrice", "totalSeats", "availableSeats", "status");
            case "trains" -> List.of("trainNo", "trainType", "departureStation", "arrivalStation", "departureTime",
                    "arrivalTime", "durationMinutes", "firstClassPrice", "secondClassPrice", "firstClassSeats",
                    "secondClassSeats", "status");
            case "hotels" -> List.of("name", "city", "address", "description", "coverImg", "lat", "lng",
                    "starRating", "avgPrice", "score", "status");
            case "rooms" -> List.of("hotelId", "roomType", "bedType", "area", "price", "totalRooms",
                    "availableRooms", "images", "facilities", "status");
            case "attractions" -> List.of("name", "city", "address", "description", "coverImg", "adultPrice",
                    "childPrice", "totalTickets", "availableTickets", "openTime", "lat", "lng", "officialUrl",
                    "sourceName", "dataCheckedDate", "status");
            case "destinations" -> List.of("slug", "name", "country", "tag", "keywords", "img", "desc", "intro",
                    "highlights", "culture", "bestSeason", "transport", "sourceName", "sourceUrl", "sortOrder",
                    "status");
            default -> throw new RuntimeException("不支持的导入类型：" + type);
        };
    }

    private String csvTemplateSample(String type) {
        return switch (type) {
            case "flights" -> "CA1001,中国国际航空,北京,上海,2026-06-01T08:00:00,2026-06-01T10:00:00,680,2180,200,120,1";
            case "trains" -> "G1001,G,北京南,上海虹桥,2026-06-01T08:00:00,2026-06-01T12:30:00,270,880,553,80,420,1";
            case "hotels" -> "城市花园酒店,上海,上海市黄浦区示例路1号,近地铁商务酒店,https://example.com/hotel.jpg,31.2304,121.4737,4,520,4.6,1";
            case "rooms" -> "1,豪华大床房,1张大床,38,688,20,12,\"[\"\"/images/room.jpg\"\"]\",\"[\"\"早餐\"\",\"\"洗衣房\"\"]\",1";
            case "attractions" -> "示例景区,杭州,杭州市示例路1号,城市观光景区,https://example.com/scenic.jpg,80,40,1000,800,08:00-18:00,30.2741,120.1551,https://example.com,景区官网,2026-05-30,1";
            case "destinations" -> "dali,大理,中国,风花雪月,洱海|古城|苍山,/images/seed/dali.svg,苍山洱海与古城生活交织,适合慢旅行的城市,洱海骑行|古城夜游,白族文化,春秋季,高铁到大理站,公开旅游资料,https://example.com,90,1";
            default -> throw new RuntimeException("不支持的导入类型：" + type);
        };
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
