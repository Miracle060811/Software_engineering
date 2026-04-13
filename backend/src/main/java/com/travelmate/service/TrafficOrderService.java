package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.dto.FlightOrderCreateDTO;
import com.travelmate.entity.TrafficOrder;

public interface TrafficOrderService extends IService<TrafficOrder> {

    /**
     * 创建机票订单并扣减库存
     * 
     * @param userId 当前用户
     * @param dto    创建参数(航班、乘车人、舱位)
     * @return 订单编号 orderNo
     */
    String createFlightOrder(Long userId, FlightOrderCreateDTO dto);

    /**
     * 创建火车票订单并扣减库存(按席别)
     * 
     * @param userId 当前用户
     * @param dto    创建参数(列车、乘车人、一等座/二等座)
     * @return 订单编号 orderNo
     */
    String createTrainOrder(Long userId, com.travelmate.dto.TrainOrderCreateDTO dto);

    /**
     * 模拟支付 (修改订单状态为出票)
     * 
     * @param orderNo 订单编号
     */
    boolean payOrder(Long userId, String orderNo);

    /**
     * 取消订单 (如果未支付可取消，如果是已支付，则走退票流程)，会归还库存
     */
    boolean cancelOrder(Long userId, String orderNo);

    /**
     * 查询我的订单列表
     */
    java.util.List<TrafficOrder> getUserOrders(Long userId);
}
