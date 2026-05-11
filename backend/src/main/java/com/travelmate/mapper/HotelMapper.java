package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Hotel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotelMapper extends BaseMapper<Hotel> {
}
