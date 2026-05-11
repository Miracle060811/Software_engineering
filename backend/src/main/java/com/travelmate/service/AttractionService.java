package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Attraction;

import java.util.List;

/**
 * 景点服务接口
 */
public interface AttractionService extends IService<Attraction> {

    /**
     * 按城市搜索景点
     *
     * @param city 城市名称
     * @return 景点列表
     */
    List<Attraction> searchAttractions(String city);

    /**
     * 购买景点门票（含乐观锁扣减库存）
     *
     * @param userId       当前用户ID
     * @param attractionId 景点ID
     * @param count        购买数量
     * @param guestName    游客姓名
     * @param guestPhone   游客手机号
     * @return 订单号
     */
    String buyTicket(Long userId, Long attractionId, Integer count,
            String guestName, String guestPhone);
}
