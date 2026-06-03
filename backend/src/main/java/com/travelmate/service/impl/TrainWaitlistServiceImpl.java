package com.travelmate.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.dto.TrainWaitlistCreateDTO;
import com.travelmate.entity.Passenger;
import com.travelmate.entity.Train;
import com.travelmate.entity.TrainWaitlist;
import com.travelmate.mapper.PassengerMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.mapper.TrainWaitlistMapper;
import com.travelmate.service.TrainWaitlistService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class TrainWaitlistServiceImpl extends ServiceImpl<TrainWaitlistMapper, TrainWaitlist>
        implements TrainWaitlistService {
    private final TrainMapper trainMapper;
    private final PassengerMapper passengerMapper;

    public TrainWaitlistServiceImpl(TrainMapper trainMapper, PassengerMapper passengerMapper) {
        this.trainMapper = trainMapper;
        this.passengerMapper = passengerMapper;
    }

    @Override
    public Long createWaitlist(Long userId, TrainWaitlistCreateDTO dto) {
        if (dto == null) {
            throw new RuntimeException("候补信息不能为空");
        }
        Passenger passenger = passengerMapper.selectById(dto.getPassengerId());
        if (passenger == null || !passenger.getUserId().equals(userId)) {
            throw new RuntimeException("乘车人选择错误或不存在");
        }

        Train train = dto.getTrainId() == null ? null : trainMapper.selectById(dto.getTrainId());
        TrainWaitlist item = new TrainWaitlist();
        item.setUserId(userId);
        item.setTrainId(dto.getTrainId());
        item.setTrainNo(firstText(dto.getTrainNo(), train == null ? null : train.getTrainNo()));
        item.setDepartureStation(firstText(dto.getDepartureStation(), train == null ? null : train.getDepartureStation()));
        item.setArrivalStation(firstText(dto.getArrivalStation(), train == null ? null : train.getArrivalStation()));
        item.setDepartureTime(parseDateTime(firstText(dto.getDepartureTime(),
                train == null || train.getDepartureTime() == null ? null : train.getDepartureTime().toString())));
        item.setSeatType(firstText(dto.getSeatType(), "SecondClass"));
        item.setTicketCount(normalizeCount(dto.getTicketCount()));
        item.setPassengerName(passenger.getName());
        item.setPassengerIdCard(passenger.getIdCard());
        item.setStatus(0);
        item.setCreateTime(LocalDateTime.now());

        if (!StringUtils.hasText(item.getTrainNo()) || !StringUtils.hasText(item.getDepartureStation())
                || !StringUtils.hasText(item.getArrivalStation()) || item.getDepartureTime() == null) {
            throw new RuntimeException("候补车次信息不完整");
        }

        save(item);
        return item.getId();
    }

    private static String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    private static LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            try {
                return LocalDateTime.parse(value.trim().replace(" ", "T"));
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static int normalizeCount(Integer count) {
        int value = count == null ? 1 : count;
        if (value <= 0 || value > 10) {
            throw new RuntimeException("候补票数必须在1-10之间");
        }
        return value;
    }
}
