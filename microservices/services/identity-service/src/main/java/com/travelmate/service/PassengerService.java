package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Passenger;

import java.util.List;

public interface PassengerService extends IService<Passenger> {

    /**
     * 获取指定用户的常用旅客列表
     */
    List<Passenger> getPassengerList(Long userId);

    /**
     * 添加常用旅客
     */
    boolean addPassenger(Passenger passenger);

    /**
     * 删除常用旅客
     */
    boolean deletePassenger(Long id, Long userId);
}
