package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.HotelOrder;
import com.travelmate.integration.NotificationGateway;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.service.HotelRoomStockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/local/admin/orders")
public class InternalAdminHotelOrderController {
    private final HotelOrderMapper orders;
    private final HotelRoomMapper rooms;
    private final HotelRoomStockService stockService;
    private final NotificationGateway notifications;
    private final String token;

    public InternalAdminHotelOrderController(HotelOrderMapper orders, HotelRoomMapper rooms,
                                             HotelRoomStockService stockService, NotificationGateway notifications,
                                             @Value("${app.internal-service-token}") String token) {
        this.orders = orders; this.rooms = rooms; this.stockService = stockService;
        this.notifications = notifications; this.token = token;
    }

    @PostMapping("/{orderNo}/refund/approve")
    @Transactional(rollbackFor = Exception.class)
    public String approve(@PathVariable String orderNo, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); HotelOrder order = find(orderNo);
        if (Integer.valueOf(4).equals(order.getStatus())) return "订单已取消或已退款，无需重复处理";
        if (Integer.valueOf(3).equals(order.getStatus())) conflict("已完成的酒店订单不能直接退款");
        if (!Integer.valueOf(5).equals(order.getStatus())) conflict("只有已提交退款申请的酒店订单可以办理退款");
        if (orders.markRefundApproved(orderNo) == 0) conflict("订单状态已变化，请刷新后重试");
        rooms.returnRoom(order.getRoomId(), order.getRoomCount() == null ? 1 : order.getRoomCount());
        stockService.syncWithDatabase(order.getRoomId());
        notifications.publish(order.getUserId(), "hotel_order", "酒店退款已通过",
                "订单 " + orderNo + " 已退款，房间库存已归还。", "/my-orders?tab=hotel");
        return "退款审批已通过";
    }

    @PostMapping("/{orderNo}/refund/reject")
    @Transactional(rollbackFor = Exception.class)
    public String reject(@PathVariable String orderNo, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); HotelOrder order = find(orderNo);
        if (orders.markRefundRejected(orderNo) == 0) conflict("只有退款申请中的酒店订单可以驳回");
        notifications.publish(order.getUserId(), "hotel_order", "酒店退款申请被驳回",
                "订单 " + orderNo + " 未通过退款审核，订单已恢复为已支付。", "/my-orders?tab=hotel");
        return "退款申请已驳回";
    }

    private HotelOrder find(String orderNo) {
        HotelOrder order = orders.selectOne(new LambdaQueryWrapper<HotelOrder>().eq(HotelOrder::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        return order;
    }
    private void verify(String supplied) { if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务令牌无效"); }
    private void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
