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
        if (passenger == null || passenger.getUserId() == null) {
            throw new RuntimeException("用户信息无效");
        }
        String name = passenger.getName() == null ? "" : passenger.getName().trim();
        String idCard = passenger.getIdCard() == null ? "" : passenger.getIdCard().trim().toUpperCase();
        String phone = passenger.getPhone() == null ? "" : passenger.getPhone().trim();
        if (name.isEmpty() || name.length() > 50) {
            throw new RuntimeException("旅客姓名格式无效");
        }
        if (!idCard.matches("(?:\\d{17}[0-9X]|[A-Z][A-Z0-9]{5,19})")) {
            throw new RuntimeException("证件号格式无效");
        }
        if (!phone.matches("1\\d{10}")) {
            throw new RuntimeException("手机号格式无效");
        }
        if (passenger.getType() != null && passenger.getType() != 0 && passenger.getType() != 1) {
            throw new RuntimeException("旅客类型无效");
        }
        if (count(new LambdaQueryWrapper<Passenger>()
                .eq(Passenger::getUserId, passenger.getUserId())
                .eq(Passenger::getIdCard, idCard)) > 0) {
            throw new RuntimeException("该证件旅客已存在");
        }
        passenger.setName(name);
        passenger.setIdCard(idCard);
        passenger.setPhone(phone);
        passenger.setType(passenger.getType() == null ? 0 : passenger.getType());
        passenger.setCreateTime(LocalDateTime.now());
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
