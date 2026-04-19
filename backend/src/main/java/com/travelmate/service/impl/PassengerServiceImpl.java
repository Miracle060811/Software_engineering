package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Passenger;
import com.travelmate.mapper.PassengerMapper;
import com.travelmate.service.PassengerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PassengerServiceImpl extends ServiceImpl<PassengerMapper, Passenger> implements PassengerService {

    @Override
    public List<Passenger> getPassengerList(Long userId) {
        LambdaQueryWrapper<Passenger> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Passenger::getUserId, userId);
        wrapper.orderByDesc(Passenger::getCreateTime);
        return list(wrapper);
    }

    @Override
    public boolean addPassenger(Passenger passenger) {
        passenger.setCreateTime(LocalDateTime.now());
        // 证件信息脱敏或者校验可以在这里做
        return save(passenger);
    }

    @Override
    public boolean deletePassenger(Long id, Long userId) {
        LambdaQueryWrapper<Passenger> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Passenger::getId, id)
                .eq(Passenger::getUserId, userId);

        // Mybatis-Plus 会将此操作替换为 logic delete
        return remove(wrapper);
    }
}
