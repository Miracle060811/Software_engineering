package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.dto.TrainLiveSyncStatus;
import com.travelmate.entity.Train;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.TrainLiveSyncService;
import com.travelmate.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainServiceImpl extends ServiceImpl<TrainMapper, Train> implements TrainService {
    private static final int DEFAULT_RESULT_LIMIT = 10;
    private static final int MAX_RESULT_LIMIT = 20;

    @Autowired
    private TrainLiveSyncService trainLiveSyncService;

    @Autowired
    private TrainStationResolver trainStationResolver;

    @Override
    public List<Train> searchTrains(String depStation, String arrStation, String depDate) {
        return searchTrains(depStation, arrStation, depDate, 0, DEFAULT_RESULT_LIMIT);
    }

    @Override
    public List<Train> searchTrains(String depStation, String arrStation, String depDate, Integer offset, Integer limit) {
        int safeOffset = normalizeOffset(offset);
        int safeLimit = normalizeLimit(limit);
        int collectTarget = safeOffset + safeLimit;

        if (StringUtils.hasText(depStation) && StringUtils.hasText(arrStation) && StringUtils.hasText(depDate)) {
            boolean cityInput = trainStationResolver.isCityInput(depStation)
                    || trainStationResolver.isCityInput(arrStation);
            // 本地已有数据时立即返回，避免 12306 页面异常拖慢正常搜索。
            List<Train> localTrains = queryLocalTrains(depStation, arrStation, depDate);
            if (!localTrains.isEmpty()) {
                if (cityInput) {
                    trainLiveSyncService.syncIfSupported(depStation, arrStation, depDate);
                }
                return pageList(localTrains, safeOffset, safeLimit);
            }

            // 当天没有库存时，优先使用本地路线模板换算日期，保证搜索接口不被外部页面阻塞。
            List<Train> localDemoTrains = demoFallbackTrains(depStation, arrStation, depDate);
            if (!localDemoTrains.isEmpty()) {
                if (cityInput) {
                    trainLiveSyncService.syncIfSupported(depStation, arrStation, depDate);
                }
                return pageList(localDemoTrains, safeOffset, safeLimit);
            }

            Map<String, Train> liveMatches = new LinkedHashMap<>();
            for (TrainStationResolver.RouteCandidate route : trainStationResolver.routeCandidates(depStation, arrStation)) {
                TrainLiveSyncStatus syncStatus = trainLiveSyncService.syncIfSupported(
                        route.depStation(), route.arrStation(), depDate);
                if (cityInput) {
                    break;
                }
                if (syncStatus.isSynced()) {
                    for (Train train : trainLiveSyncService.getCachedLiveTrains(
                            route.depStation(), route.arrStation(), depDate)) {
                        liveMatches.putIfAbsent(trainKey(train), train);
                    }
                    if (liveMatches.size() >= collectTarget) {
                        return pageLiveResults(liveMatches, safeOffset, safeLimit);
                    }
                }
            }
            if (!liveMatches.isEmpty()) {
                return pageLiveResults(liveMatches, safeOffset, safeLimit);
            }
        }

        List<Train> trains = queryLocalTrains(depStation, arrStation, depDate);
        if (trains.isEmpty() && StringUtils.hasText(depStation) && StringUtils.hasText(arrStation)
                && StringUtils.hasText(depDate)) {
            return pageList(demoFallbackTrains(depStation, arrStation, depDate), safeOffset, safeLimit);
        }
        return pageList(trains, safeOffset, safeLimit);
    }

    private List<Train> queryLocalTrains(String depStation, String arrStation, String depDate) {
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Train::getStatus, 1);

        if (StringUtils.hasText(depStation)) {
            List<String> depStations = trainStationResolver.stationsFor(depStation);
            wrapper.and(w -> w.in(Train::getDepartureStation, depStations)
                    .or()
                    .like(Train::getDepartureStation, depStation.trim()));
        }
        if (StringUtils.hasText(arrStation)) {
            List<String> arrStations = trainStationResolver.stationsFor(arrStation);
            wrapper.and(w -> w.in(Train::getArrivalStation, arrStations)
                    .or()
                    .like(Train::getArrivalStation, arrStation.trim()));
        }
        if (StringUtils.hasText(depDate)) {
            wrapper.likeRight(Train::getDepartureTime, depDate);
        }

        return list(wrapper.orderByAsc(Train::getDepartureTime));
    }

    private List<Train> demoFallbackTrains(String depStation, String arrStation, String depDate) {
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(depDate);
        } catch (Exception ex) {
            return java.util.List.of();
        }

        List<Train> cached = this.list(new LambdaQueryWrapper<Train>()
                .eq(Train::getStatus, 1)
                .eq(Train::getDepartureStation, depStation)
                .eq(Train::getArrivalStation, arrStation)
                .orderByAsc(Train::getDepartureTime)
                .last("LIMIT 6"));

        return cached.stream()
                .map(train -> shiftTrainDate(train, targetDate))
                .toList();
    }

    private Train shiftTrainDate(Train source, LocalDate targetDate) {
        Train copy = new Train();
        copy.setId(source.getId());
        copy.setTrainNo(source.getTrainNo());
        copy.setTrainType(source.getTrainType());
        copy.setDepartureStation(source.getDepartureStation());
        copy.setArrivalStation(source.getArrivalStation());

        LocalDateTime sourceDeparture = source.getDepartureTime();
        LocalDateTime sourceArrival = source.getArrivalTime();
        if (sourceDeparture != null) {
            LocalDateTime shiftedDeparture = LocalDateTime.of(targetDate, sourceDeparture.toLocalTime());
            copy.setDepartureTime(shiftedDeparture);
            if (sourceArrival != null) {
                copy.setArrivalTime(shiftedDeparture.plusMinutes(java.time.Duration.between(sourceDeparture, sourceArrival).toMinutes()));
            }
        }

        copy.setDurationMinutes(source.getDurationMinutes());
        copy.setFirstClassPrice(source.getFirstClassPrice());
        copy.setSecondClassPrice(source.getSecondClassPrice());
        copy.setFirstClassSeats(source.getFirstClassSeats());
        copy.setSecondClassSeats(source.getSecondClassSeats());
        copy.setBusinessClassSeats(source.getBusinessClassSeats());
        copy.setHardSeatSeats(source.getHardSeatSeats());
        copy.setNoSeatSeats(source.getNoSeatSeats());
        copy.setBusinessSeatText(source.getBusinessSeatText());
        copy.setFirstClassSeatText(source.getFirstClassSeatText());
        copy.setSecondClassSeatText(source.getSecondClassSeatText());
        copy.setHardSeatText(source.getHardSeatText());
        copy.setNoSeatText(source.getNoSeatText());
        copy.setLiveOnly(source.getLiveOnly());
        copy.setDataSource(source.getDataSource());
        copy.setStatus(source.getStatus());
        return copy;
    }

    private static String trainKey(Train train) {
        return (train.getTrainNo() == null ? "" : train.getTrainNo()) + "|"
                + (train.getDepartureStation() == null ? "" : train.getDepartureStation()) + "|"
                + (train.getArrivalStation() == null ? "" : train.getArrivalStation()) + "|"
                + (train.getDepartureTime() == null ? "" : train.getDepartureTime());
    }

    private static List<Train> pageLiveResults(Map<String, Train> liveMatches, int offset, int limit) {
        return liveMatches.values().stream()
                .sorted(Comparator.comparing(Train::getDepartureTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    private static List<Train> pageList(List<Train> trains, int offset, int limit) {
        if (trains == null || trains.isEmpty() || offset >= trains.size()) {
            return List.of();
        }
        return trains.stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    private static int normalizeOffset(Integer offset) {
        return Math.max(0, offset == null ? 0 : offset);
    }

    private static int normalizeLimit(Integer limit) {
        int value = limit == null ? DEFAULT_RESULT_LIMIT : limit;
        if (value <= 0) {
            return DEFAULT_RESULT_LIMIT;
        }
        return Math.min(value, MAX_RESULT_LIMIT);
    }

    /**
     * 算法难点加分项 - "智能中转方案拼接"
     * 如果直达票没票，或者干脆没有班次，给用户一条可以拆分成两段买的车票。
     */
    @Override
    public List<java.util.List<Train>> getTransferPlan(String depStation, String arrStation, String depDate) {
        List<java.util.List<Train>> result = new java.util.ArrayList<>();

        // 1. 获取所有"从出发地"出去的第一程车
        List<Train> firstLegMatches = this.searchTrains(depStation, null, depDate);

        // 2. 根据这些第一程的到达地去找是否有去"最终目的地"的第二程
        // 条件：接驳等待时间合理比如 >= 30分钟且 <= 5小时。
        for (Train leg1 : firstLegMatches) {
            String midStation = leg1.getArrivalStation();

            // 为了防止无限长或者死循环只找发车时间 > 第一段到达时间 30分钟 的车 (给乘车换乘余量)
            List<Train> leg2Matches = this.list(new LambdaQueryWrapper<Train>()
                    .eq(Train::getDepartureStation, midStation)
                    .eq(Train::getArrivalStation, arrStation)
                    .gt(Train::getDepartureTime, leg1.getArrivalTime().plusMinutes(30))
                    .lt(Train::getDepartureTime, leg1.getArrivalTime().plusHours(5))
                    // 同理保证余票
                    .and(w -> w.gt(Train::getFirstClassSeats, 0).or().gt(Train::getSecondClassSeats, 0)));

            if (leg2Matches != null && !leg2Matches.isEmpty()) {
                // 每抓到满足条件的一段，就拼成一个List装进方案库
                for (Train leg2 : leg2Matches) {
                    List<Train> plan = new java.util.ArrayList<>();
                    plan.add(leg1);
                    plan.add(leg2);
                    result.add(plan);
                }
            }
        }
        return result;
    }
}
