package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Passenger;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PassengerMapper extends BaseMapper<Passenger> {
}
