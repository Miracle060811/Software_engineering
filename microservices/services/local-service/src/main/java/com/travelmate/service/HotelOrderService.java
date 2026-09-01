package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.dto.HotelOrderCreateDTO;
import com.travelmate.entity.HotelOrder;

import java.util.List;

/**
 * 酒店订单服务接口
 */
public interface HotelOrderService extends IService<HotelOrder> {

    /**
     * 创建酒店订单（含乐观锁扣减房间库存）
     *
     * @param userId 当前用户ID
     * @param dto    订单创建请求
     * @return 订单号
     */
    String createOrder(Long userId, HotelOrderCreateDTO dto);

    /**
     * 模拟支付订单（状态 0→1）
     *
     * @param userId  当前用户ID
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean payOrder(Long userId, String orderNo);

    /**
     * 取消订单（状态 0→4，归还房间库存）
     *
     * @param userId  当前用户ID
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean cancelOrder(Long userId, String orderNo);

    /**
     * 申请退款，进入后台人工处理队列。
     */
    boolean requestRefund(Long userId, String orderNo);

    /**
     * 查询单个酒店订单详情。
     */
    HotelOrder getOrderDetail(Long userId, String orderNo);

    /**
     * 查询用户所有酒店订单
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    List<HotelOrder> getUserOrders(Long userId);
}
