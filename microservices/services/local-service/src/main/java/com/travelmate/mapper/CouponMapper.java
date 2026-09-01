package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
    @Update("UPDATE tm_coupon SET stock = stock - 1 WHERE id = #{id} AND status = 0 AND stock > 0")
    int deductStock(@Param("id") Long id);
}
