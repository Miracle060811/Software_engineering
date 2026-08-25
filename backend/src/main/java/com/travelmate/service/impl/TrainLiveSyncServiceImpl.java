package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.dto.TrainBrowserTicket;
import com.travelmate.dto.TrainLiveSyncStatus;
import com.travelmate.entity.Train;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.TrainBrowserSyncService;
import com.travelmate.service.TrainLiveSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;

@Service
public class TrainLiveSyncServiceImpl implements TrainLiveSyncService {
    private static final Logger log = LoggerFactory.getLogger(TrainLiveSyncServiceImpl.class);
    private static final int MAX_SYNC_DAYS = 15;
    private static final int MAX_SYNC_TRAINS_PER_ROUTE = 30;
    private static final Duration ATTEMPT_CACHE_TTL = Duration.ofMinutes(8);

    private static final String SOURCE_12306_PAGE = "12306_PAGE";
    private static final String SOURCE_LOCAL_DEMO_CACHE = "LOCAL_DEMO_CACHE";
    private static final String SOURCE_LOCAL_DATABASE = "LOCAL_DATABASE";
    private static final Set<String> CITY_ALIASES = Set.of("北京", "上海", "广州", "杭州", "成都", "重庆");

    private final TrainMapper trainMapper;
    private final TrainBrowserSyncService trainBrowserSyncService;
    private final AtomicReference<TrainLiveSyncStatus> lastStatus;
    private final Map<String, TrainLiveSyncStatus> recentStatuses;
    private final Map<String, List<Train>> recentLiveTrains;

    @Value("${train.live-sync.enabled:true}")
    private boolean enabled;

    private static final Map<String, Station> STATIONS = new LinkedHashMap<>();

    static {
        station("北京南", "VNP");
        station("北京西", "BXP");
        station("上海虹桥", "AOH");
        station("广州南", "IZQ");
        station("杭州东", "HGH");
        station("成都东", "ICW");
        station("重庆北", "CUW");

        // 城市输入没有唯一站名时，使用主要客运站作为 12306 查询入口。
        stationAlias("北京", "北京南", "VNP");
        stationAlias("上海", "上海虹桥", "AOH");
        stationAlias("广州", "广州南", "IZQ");
        stationAlias("杭州", "杭州东", "HGH");
        stationAlias("成都", "成都东", "ICW");
        stationAlias("重庆", "重庆北", "CUW");
    }

    public TrainLiveSyncServiceImpl(TrainMapper trainMapper, TrainBrowserSyncService trainBrowserSyncService) {
        this.trainMapper = trainMapper;
        this.trainBrowserSyncService = trainBrowserSyncService;
        this.lastStatus = new AtomicReference<>(
                new TrainLiveSyncStatus(true, false, false, "尚未同步", null, null, SOURCE_LOCAL_DATABASE, 0, null));
        this.recentStatuses = new ConcurrentHashMap<>();
        this.recentLiveTrains = new ConcurrentHashMap<>();
    }

    @Override
    public TrainLiveSyncStatus syncIfSupported(String depStation, String arrStation, String date) {
        String normalizedDep = normalizeStation(depStation);
        String normalizedArr = normalizeStation(arrStation);
        String routeKey = routeKey(normalizedDep, normalizedArr);

        if (!enabled) {
            return remember(false, false, false, "12306 页面读取演示已关闭，展示本地数据库数据",
                    routeKey, date, SOURCE_LOCAL_DATABASE, 0);
        }
        if (!StringUtils.hasText(normalizedDep) || !StringUtils.hasText(normalizedArr) || !StringUtils.hasText(date)) {
            return remember(true, false, false, "请输入出发站、到达站和日期后再尝试读取 12306 页面",
                    routeKey, date, SOURCE_LOCAL_DATABASE, 0);
        }
        Station configuredDep = STATIONS.get(normalizedDep);
        Station configuredArr = STATIONS.get(normalizedArr);
        if (CITY_ALIASES.contains(normalizedDep) || CITY_ALIASES.contains(normalizedArr)) {
            return remember(true, true, false, "城市已映射到主要车站；输入具体车站名可尝试 12306 实时读取",
                    routeKey, date, SOURCE_LOCAL_DATABASE, 0);
        }

        LocalDate trainDate;
        try {
            trainDate = LocalDate.parse(date);
        } catch (Exception ex) {
            return remember(true, false, false, "日期格式应为 yyyy-MM-dd",
                    routeKey, date, SOURCE_LOCAL_DATABASE, 0);
        }
        LocalDate today = LocalDate.now();
        if (trainDate.isBefore(today) || trainDate.isAfter(today.plusDays(MAX_SYNC_DAYS))) {
            return remember(true, false, false, "仅允许读取未来 15 天内的 12306 公开余票页面",
                    routeKey, date, SOURCE_LOCAL_DATABASE, 0);
        }

        String attemptKey = routeKey + "|" + date;
        TrainLiveSyncStatus recent = recentStatuses.get(attemptKey);
        if (recent != null && recent.getSyncedAt() != null
                && recent.getSyncedAt().plus(ATTEMPT_CACHE_TTL).isAfter(LocalDateTime.now())) {
            lastStatus.set(recent);
            return recent;
        }

        // 未写入本地白名单的具体车站由 12306 页面中的 station_names 动态解析编码。
        Station dep = configuredDep != null ? configuredDep : new Station(normalizedDep, "");
        Station arr = configuredArr != null ? configuredArr : new Station(normalizedArr, "");
        try {
            List<TrainBrowserTicket> tickets = trainBrowserSyncService.readPublicLeftTickets(
                    dep.name, dep.code, arr.name, arr.code, trainDate);
            if (tickets.isEmpty()) {
                return remember(attemptKey, true, true, false,
                        "12306 页面未读取到可展示车次，展示本地演示缓存",
                        routeKey, date, SOURCE_LOCAL_DEMO_CACHE, 0);
            }

            List<Train> trains = tickets.stream()
                    .limit(MAX_SYNC_TRAINS_PER_ROUTE)
                    .map(ticket -> toTrain(ticket, normalizedDep, normalizedArr, trainDate))
                    .filter(train -> train.getDepartureTime() != null)
                    .toList();
            for (Train train : trains) {
                upsertTrain(train);
            }
            recentLiveTrains.put(attemptKey, trains);

            return remember(attemptKey, true, true, true,
                    "12306 页面实时读取成功，已写入/更新本地车次表",
                    routeKey, date, SOURCE_12306_PAGE, trains.size());
        } catch (Exception ex) {
            log.warn("12306 browser sync failed for {} {}: {}", routeKey, date, ex.getMessage());
            return remember(attemptKey, true, true, false,
                    "12306 页面读取失败：" + friendlyError(ex) + "；展示本地演示缓存",
                    routeKey, date, SOURCE_LOCAL_DEMO_CACHE, 0);
        }
    }

    @Override
    public TrainLiveSyncStatus getLastStatus() {
        return lastStatus.get();
    }

    @Override
    public List<Train> getCachedLiveTrains(String depStation, String arrStation, String date) {
        String attemptKey = routeKey(normalizeStation(depStation), normalizeStation(arrStation)) + "|" + date;
        TrainLiveSyncStatus recent = recentStatuses.get(attemptKey);
        if (recent == null || !recent.isSynced() || recent.getSyncedAt() == null
                || recent.getSyncedAt().plus(ATTEMPT_CACHE_TTL).isBefore(LocalDateTime.now())) {
            return List.of();
        }
        return recentLiveTrains.getOrDefault(attemptKey, List.of());
    }

    private Train toTrain(TrainBrowserTicket ticket, String depStation, String arrStation, LocalDate trainDate) {
        Train train = new Train();
        train.setTrainNo(ticket.getTrainNo());
        train.setTrainType(trainType(ticket.getTrainNo()));
        train.setDepartureStation(StringUtils.hasText(ticket.getDepartureStation()) ? ticket.getDepartureStation() : depStation);
        train.setArrivalStation(StringUtils.hasText(ticket.getArrivalStation()) ? ticket.getArrivalStation() : arrStation);
        train.setDepartureTime(parseDateTime(trainDate, ticket.getDepartureTime()));
        train.setArrivalTime(parseArrivalDateTime(train.getDepartureTime(), ticket.getArrivalTime()));
        train.setDurationMinutes(durationMinutes(ticket.getDuration(), train.getDepartureTime(), train.getArrivalTime()));
        train.setSecondClassPrice(estimatePrice(train, false));
        train.setFirstClassPrice(estimatePrice(train, true));
        train.setFirstClassSeats(seatCount(ticket.getFirstClassSeat()));
        train.setSecondClassSeats(seatCount(ticket.getSecondClassSeat()));
        train.setBusinessClassSeats(seatCount(ticket.getBusinessSeat()));
        train.setHardSeatSeats(seatCount(ticket.getHardSeat()));
        train.setNoSeatSeats(seatCount(ticket.getNoSeat()));
        train.setBusinessSeatText(ticket.getBusinessSeat());
        train.setFirstClassSeatText(ticket.getFirstClassSeat());
        train.setSecondClassSeatText(ticket.getSecondClassSeat());
        train.setHardSeatText(ticket.getHardSeat());
        train.setNoSeatText(ticket.getNoSeat());
        train.setStatus(1);
        train.setLiveOnly(false);
        train.setDataSource(SOURCE_12306_PAGE);
        return train;
    }

    private void upsertTrain(Train train) {
        Train existing = trainMapper.selectOne(new LambdaQueryWrapper<Train>()
                .eq(Train::getTrainNo, train.getTrainNo())
                .eq(Train::getDepartureStation, train.getDepartureStation())
                .eq(Train::getArrivalStation, train.getArrivalStation())
                .eq(Train::getDepartureTime, train.getDepartureTime())
                .last("LIMIT 1"));
        if (existing == null) {
            try {
                trainMapper.insert(train);
            } catch (DuplicateKeyException ex) {
                Train sameDeparture = trainMapper.selectOne(new LambdaQueryWrapper<Train>()
                        .eq(Train::getTrainNo, train.getTrainNo())
                        .eq(Train::getDepartureTime, train.getDepartureTime())
                        .last("LIMIT 1"));
                if (sameDeparture == null) {
                    throw ex;
                }
                train.setId(sameDeparture.getId());
                trainMapper.updateById(train);
            }
            return;
        }
        train.setId(existing.getId());
        trainMapper.updateById(train);
    }

    private TrainLiveSyncStatus remember(boolean enabled, boolean supported, boolean synced,
                                         String message, String route, String date, String dataSource, int trainCount) {
        TrainLiveSyncStatus status = new TrainLiveSyncStatus(
                enabled, supported, synced, message, route, date, dataSource, trainCount, LocalDateTime.now());
        lastStatus.set(status);
        return status;
    }

    private TrainLiveSyncStatus remember(String attemptKey, boolean enabled, boolean supported, boolean synced,
                                         String message, String route, String date, String dataSource, int trainCount) {
        TrainLiveSyncStatus status = remember(enabled, supported, synced, message, route, date, dataSource, trainCount);
        recentStatuses.put(attemptKey, status);
        return status;
    }

    private String friendlyError(Exception ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "浏览器自动化异常";
        }
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }

    private static LocalDateTime parseDateTime(LocalDate date, String time) {
        try {
            return LocalDateTime.of(date, LocalTime.parse(time));
        } catch (Exception ex) {
            return null;
        }
    }

    private static LocalDateTime parseArrivalDateTime(LocalDateTime departure, String arrivalTime) {
        if (departure == null) {
            return null;
        }
        try {
            LocalDateTime arrival = LocalDateTime.of(departure.toLocalDate(), LocalTime.parse(arrivalTime));
            return arrival.isBefore(departure) ? arrival.plusDays(1) : arrival;
        } catch (Exception ex) {
            return departure;
        }
    }

    private static int durationMinutes(String value, LocalDateTime departure, LocalDateTime arrival) {
        if (StringUtils.hasText(value)) {
            String normalized = value.trim();
            if (normalized.matches("\\d{1,2}:\\d{2}")) {
                String[] parts = normalized.split(":");
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?:(\\d+)小时)?(?:(\\d+)分)?")
                    .matcher(normalized);
            if (matcher.matches()) {
                int hours = StringUtils.hasText(matcher.group(1)) ? Integer.parseInt(matcher.group(1)) : 0;
                int minutes = StringUtils.hasText(matcher.group(2)) ? Integer.parseInt(matcher.group(2)) : 0;
                if (hours > 0 || minutes > 0) {
                    return hours * 60 + minutes;
                }
            }
        }
        if (departure != null && arrival != null) {
            return (int) Duration.between(departure, arrival).toMinutes();
        }
        return 0;
    }

    private static BigDecimal estimatePrice(Train train, boolean firstClass) {
        int minutes = Math.max(train.getDurationMinutes() == null ? 0 : train.getDurationMinutes(), 30);
        String prefix = StringUtils.hasText(train.getTrainNo())
                ? train.getTrainNo().substring(0, 1).toUpperCase(Locale.ROOT)
                : "";
        BigDecimal perMinute = switch (prefix) {
            case "G" -> new BigDecimal("1.45");
            case "D", "C" -> new BigDecimal("1.05");
            case "Z", "T", "K" -> new BigDecimal("0.45");
            default -> new BigDecimal("0.60");
        };
        BigDecimal secondClass = perMinute.multiply(BigDecimal.valueOf(minutes)).max(new BigDecimal("20.00"));
        BigDecimal price = firstClass ? secondClass.multiply(new BigDecimal("1.60")) : secondClass;
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private static int seatCount(String value) {
        if (!StringUtils.hasText(value) || "--".equals(value) || "*".equals(value)
                || "无".equals(value) || "候补".equals(value)) {
            return 0;
        }
        if ("有".equals(value)) {
            return 30;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String trainType(String trainNo) {
        String prefix = StringUtils.hasText(trainNo) ? trainNo.substring(0, 1).toUpperCase(Locale.ROOT) : "";
        return switch (prefix) {
            case "G" -> "高铁";
            case "D" -> "动车";
            case "C" -> "城际";
            case "Z" -> "直达";
            case "T" -> "特快";
            case "K" -> "普快";
            default -> "火车";
        };
    }

    private static String normalizeStation(String station) {
        return StringUtils.hasText(station) ? station.trim() : "";
    }

    private static String routeKey(String depStation, String arrStation) {
        return normalizeStation(depStation) + "->" + normalizeStation(arrStation);
    }

    private static void station(String name, String code) {
        STATIONS.put(name, new Station(name, code));
    }

    private static void stationAlias(String input, String stationName, String code) {
        STATIONS.put(input, new Station(stationName, code));
    }

    private record Station(String name, String code) {
    }
}
