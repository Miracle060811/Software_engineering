package com.travelmate.microservices.ops;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");
    private final OpsAggregationGateway gateway;
    private final OpsLocalService localService;

    public AdminDashboardService(OpsAggregationGateway gateway, OpsLocalService localService) {
        this.gateway = gateway;
        this.localService = localService;
    }

    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        Map<String, String> sourceStatus = new LinkedHashMap<>();
        List<Map<String, Object>> degradedSources = new ArrayList<>();

        List<Map<String, Object>> users = fetch("identity-service", List.of("totalUsers", "userGrowth"),
                gateway::users, List.of(), sourceStatus, degradedSources);
        List<Map<String, Object>> trafficOrders = fetch("traffic-service",
                List.of("totalOrders", "todayOrders", "todayGmv", "dailyTrend", "hotDestinations", "orderTypeDist"),
                gateway::orders, List.of(), sourceStatus, degradedSources);
        List<Map<String, Object>> localOrders = fetch("local-service",
                List.of("totalOrders", "todayOrders", "todayGmv", "dailyTrend", "hotDestinations", "orderTypeDist"),
                gateway::localOrders, List.of(), sourceStatus, degradedSources);
        Long pendingPosts = fetch("community-service", List.of("pendingPosts", "alerts"),
                gateway::pendingPostCount, null, sourceStatus, degradedSources);
        Map<String, Object> observation = fetch("ops-service",
                List.of("onlineUsers", "qpsTrend", "latencyTrend", "recentErrors", "alerts"),
                localService::dashboardMetrics, Map.of(), sourceStatus, degradedSources);

        boolean usersAvailable = "available".equals(sourceStatus.get("identity-service"));
        boolean ordersAvailable = "available".equals(sourceStatus.get("traffic-service"))
                && "available".equals(sourceStatus.get("local-service"));
        boolean observationAvailable = "available".equals(sourceStatus.get("ops-service"));
        List<Map<String, Object>> orders = new ArrayList<>(trafficOrders);
        orders.addAll(localOrders);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", usersAvailable ? (long) users.size() : null);
        data.put("totalOrders", ordersAvailable ? (long) orders.size() : null);
        data.put("todayOrders", ordersAvailable ? countTodayOrders(orders, today) : null);
        data.put("pendingPosts", pendingPosts);
        data.put("todayGmv", ordersAvailable ? countTodayGmv(orders, today) : null);
        data.put("onlineUsers", observationAvailable ? observation.getOrDefault("onlineUsers", 0L) : null);
        data.put("dailyTrend", ordersAvailable ? buildDailyTrend(orders, today) : List.of());
        data.put("hotDestinations", ordersAvailable ? buildHotDestinations(orders) : List.of());
        data.put("orderTypeDist", ordersAvailable ? buildOrderTypeDist(orders) : unavailableOrderTypeDist());
        data.put("userGrowth", usersAvailable ? buildUserGrowth(users, today) : List.of());
        data.put("qpsTrend", observationAvailable ? listValue(observation, "qpsTrend") : List.of());
        data.put("latencyTrend", observationAvailable ? listValue(observation, "latencyTrend") : List.of());
        data.put("recentErrors", observationAvailable ? listValue(observation, "recentErrors") : List.of());
        data.put("alerts", buildAlerts(pendingPosts, observation, observationAvailable, degradedSources));
        data.put("partial", !degradedSources.isEmpty());
        data.put("sourceStatus", sourceStatus);
        data.put("degradedSources", degradedSources);
        data.put("metricCoverage", Map.of(
                "totalUsers", List.of("identity-service"),
                "userGrowth", List.of("identity-service"),
                "pendingPosts", List.of("community-service"),
                "orders", List.of("traffic-service", "local-service"),
                "observability", List.of("ops-service")));
        return data;
    }

    private <T> T fetch(String source, List<String> fields, Supplier<T> supplier, T fallback,
                        Map<String, String> sourceStatus, List<Map<String, Object>> degradedSources) {
        try {
            T value = supplier.get();
            sourceStatus.put(source, "available");
            return value == null ? fallback : value;
        } catch (RuntimeException exception) {
            sourceStatus.put(source, "unavailable");
            addDegradation(degradedSources, source, fields, "服务暂不可用，相关字段已安全降级");
            return fallback;
        }
    }

    private void addDegradation(List<Map<String, Object>> degradedSources, String source,
                                List<String> fields, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("source", source);
        item.put("fields", fields);
        item.put("reason", reason);
        degradedSources.add(item);
    }

    private long countTodayOrders(List<Map<String, Object>> orders, LocalDate today) {
        return orders.stream().map(order -> dateValue(order.get("createTime")))
                .filter(today::equals).count();
    }

    private BigDecimal countTodayGmv(List<Map<String, Object>> orders, LocalDate today) {
        return orders.stream()
                .filter(order -> today.equals(dateValue(order.get("createTime"))))
                .filter(order -> List.of(1, 2, 3).contains(integerValue(order.get("status"))))
                .map(order -> decimalValue(order.get("amount")))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> buildDailyTrend(List<Map<String, Object>> orders, LocalDate today) {
        Map<LocalDate, Long> counts = orders.stream()
                .map(order -> dateValue(order.get("createTime")))
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today))
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            trend.add(Map.of("day", date.format(DAY_LABEL), "count", counts.getOrDefault(date, 0L)));
        }
        return trend;
    }

    private List<Map<String, Object>> buildUserGrowth(List<Map<String, Object>> users, LocalDate today) {
        Map<LocalDate, Long> counts = users.stream()
                .map(user -> dateValue(user.get("createTime")))
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today))
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            trend.add(Map.of("day", date.format(DAY_LABEL), "count", counts.getOrDefault(date, 0L)));
        }
        return trend;
    }

    private List<Map<String, Object>> buildOrderTypeDist(List<Map<String, Object>> orders) {
        long flight = orders.stream().filter(order -> Objects.equals(integerValue(order.get("orderType")), 0)).count();
        long train = orders.stream().filter(order -> Objects.equals(integerValue(order.get("orderType")), 1)).count();
        long hotel = orders.stream().filter(order -> "hotel".equals(textValue(order.get("category")))).count();
        long attraction = orders.stream().filter(order -> "attraction".equals(textValue(order.get("category")))).count();
        long tour = orders.stream().filter(order -> "tour".equals(textValue(order.get("category")))).count();
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("name", "机票", "value", flight));
        result.add(Map.of("name", "火车票", "value", train));
        result.add(Map.of("name", "酒店", "value", hotel));
        result.add(Map.of("name", "景点门票", "value", attraction));
        result.add(Map.of("name", "跟团游", "value", tour));
        return result;
    }

    private List<Map<String, Object>> unavailableOrderTypeDist() {
        return List.of(nullableValue("机票", null), nullableValue("火车票", null), nullableValue("酒店", null));
    }

    private Map<String, Object> nullableValue(String name, Object value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private List<Map<String, Object>> buildHotDestinations(List<Map<String, Object>> orders) {
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> order : orders) {
            Integer type = integerValue(order.get("orderType"));
            String destination = textValue(order.get("destination"));
            if (destination == null) {
                destination = Objects.equals(type, 0)
                        ? textValue(order.get("arrivalCity"))
                        : Objects.equals(type, 1) ? textValue(order.get("arrivalStation")) : null;
            }
            if (destination != null) {
                counts.merge(destination, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(10)
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> buildAlerts(Long pendingPosts, Map<String, Object> observation,
                                                   boolean observationAvailable,
                                                   List<Map<String, Object>> degradedSources) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        if (observationAvailable && longValue(observation.get("errorLogsToday")) > 0) {
            long count = longValue(observation.get("errorLogsToday"));
            alerts.add(Map.of("level", "danger", "title", "系统异常日志告警",
                    "message", "今日检测到 " + count + " 条失败操作日志，请尽快排查。"));
        }
        if (pendingPosts != null && pendingPosts > 0) {
            alerts.add(Map.of("level", "info", "title", "内容审核待处理",
                    "message", "待审核游记还有 " + pendingPosts + " 篇。"));
        }
        if (!degradedSources.isEmpty()) {
            alerts.add(Map.of("level", "warning", "title", "部分统计暂不可用",
                    "message", "部分统计暂未完成更新，请稍后刷新。"));
        }
        if (alerts.isEmpty()) {
            alerts.add(Map.of("level", "success", "title", "系统运行稳定", "message", "当前未发现高优先级异常。"));
        }
        return alerts;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private LocalDate dateValue(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof LocalDateTime dateTime) return dateTime.toLocalDate();
        if (value instanceof OffsetDateTime dateTime) return dateTime.toLocalDate();
        if (value == null) return null;
        String text = value.toString().trim();
        if (text.isEmpty()) return null;
        try {
            return LocalDateTime.parse(text).toLocalDate();
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(text).toLocalDate();
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    return LocalDate.parse(text);
                } catch (DateTimeParseException invalid) {
                    return null;
                }
            }
        }
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return null;
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return 0L;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException invalid) {
            return 0L;
        }
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private String textValue(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
