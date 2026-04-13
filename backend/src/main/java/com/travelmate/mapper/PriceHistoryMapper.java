package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.PriceHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PriceHistoryMapper extends BaseMapper<PriceHistory> {
}