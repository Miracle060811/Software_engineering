package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_passenger")
public class Passenger {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID (当前登录用户)
     */
    private Long userId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 身份证号/护照号
     */
    private String idCard;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 旅客类型: 0-成人, 1-儿童
     */
    private Integer type;

    private LocalDateTime createTime;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
