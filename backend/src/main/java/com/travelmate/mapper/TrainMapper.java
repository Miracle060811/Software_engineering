package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Train;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrainMapper extends BaseMapper<Train> {

    /** 扣减一等座防超卖 */
    @Update("UPDATE tm_train SET first_class_seats = first_class_seats - #{count} WHERE id = #{id} AND status = 1 AND first_class_seats >= #{count}")
    int deductFirstClassSeat(@Param("id") Long id, @Param("count") Integer count);

    /** 扣减二等座防超卖 */
    @Update("UPDATE tm_train SET second_class_seats = second_class_seats - #{count} WHERE id = #{id} AND status = 1 AND second_class_seats >= #{count}")
    int deductSecondClassSeat(@Param("id") Long id, @Param("count") Integer count);

    /** 归还一等座 */
    @Update("UPDATE tm_train SET first_class_seats = first_class_seats + #{count} WHERE id = #{id}")
    int returnFirstClassSeat(@Param("id") Long id, @Param("count") Integer count);

    /** 归还二等座 */
    @Update("UPDATE tm_train SET second_class_seats = second_class_seats + #{count} WHERE id = #{id}")
    int returnSecondClassSeat(@Param("id") Long id, @Param("count") Integer count);
}
