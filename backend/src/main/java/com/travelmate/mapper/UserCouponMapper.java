package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    @Update("UPDATE tm_user_coupon SET status = 1, used_time = #{usedTime} " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 0")
    int markUsed(@Param("id") Long id, @Param("userId") Long userId, @Param("usedTime") LocalDateTime usedTime);

    @Update("UPDATE tm_user_coupon SET status = 2 " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 0")
    int markExpired(@Param("id") Long id, @Param("userId") Long userId);
}
