package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.AttractionOrder;
import com.travelmate.mapper.AttractionMapper;
import com.travelmate.mapper.AttractionOrderMapper;
import com.travelmate.service.AttractionService;
import com.travelmate.integration.NotificationGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AttractionServiceImpl extends ServiceImpl<AttractionMapper, Attraction>
        implements AttractionService {

    @Autowired
    private NotificationGateway notificationGateway;

    @Autowired
    private AttractionOrderMapper attractionOrderMapper;

    @Override
    public List<Attraction> searchAttractions(String city) {
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getStatus, 1);

        if (StringUtils.hasText(city)) {
            wrapper.eq(Attraction::getCity, city);
        }

        wrapper.orderByDesc(Attraction::getId);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String buyTicket(Long userId, Long attractionId, Integer adultCount, Integer childCount,
            String guestName, String guestPhone) {
        int adults = adultCount == null ? 0 : adultCount;
        int children = childCount == null ? 0 : childCount;
        int count = adults + children;
        if (count <= 0 || count > 10) {
            throw new RuntimeException("购买数量不合法");
        }
        if (adults < 0 || children < 0) {
            throw new RuntimeException("票数不能为负数");
        }

        // 查询景点信息
        Attraction attraction = getById(attractionId);
        if (attraction == null || attraction.getStatus() != 1) {
            throw new RuntimeException("景点不存在或已下线");
        }
        if (!StringUtils.hasText(guestName) || !StringUtils.hasText(guestPhone)) {
            throw new RuntimeException("游客姓名和手机号不能为空");
        }
        if (attraction.getAdultPrice() == null) {
            throw new RuntimeException("景点票价缺失，暂不可购买");
        }

        // 乐观锁扣减票数（支持批量购买）
        int updated = baseMapper.deductTicket(attractionId, count);
        if (updated == 0) {
            throw new RuntimeException("门票库存不足，购票失败");
        }

        // 生成景点订单号 (前缀 AT = Attraction Ticket)
        String orderNo = "AT" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        BigDecimal amount = attraction.getAdultPrice().multiply(BigDecimal.valueOf(adults))
                .add((attraction.getChildPrice() == null ? BigDecimal.ZERO : attraction.getChildPrice())
                        .multiply(BigDecimal.valueOf(children)));

        AttractionOrder order = new AttractionOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAttractionId(attractionId);
        order.setAttractionName(attraction.getName());
        order.setCity(attraction.getCity());
        order.setAdultCount(adults);
        order.setChildCount(children);
        order.setTicketCount(count);
        order.setGuestName(guestName);
        order.setGuestPhone(guestPhone);
        order.setAmount(amount);
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        order.setDeleted(0);
        attractionOrderMapper.insert(order);

        System.out.println("====== [Attraction] 购票成功，订单号: " + orderNo
                + "，景点: " + attraction.getName()
                + "，成人票: " + adults
                + "，儿童票: " + children
                + "，总数: " + count + " ======");

        notificationGateway.publish(
                userId,
                "attraction_order",
                "景点门票购买成功",
                String.format("您已成功购买 %s 门票 %d 张，订单号：%s。", attraction.getName(), count, orderNo),
                "/my-orders?tab=attraction");

        return orderNo;
    }

    @Override
    public List<AttractionOrder> getUserTicketOrders(Long userId) {
        return attractionOrderMapper.selectList(new LambdaQueryWrapper<AttractionOrder>()
                .eq(AttractionOrder::getUserId, userId)
                .eq(AttractionOrder::getDeleted, 0)
                .orderByDesc(AttractionOrder::getCreateTime));
    }

    @Override
    public AttractionOrder getTicketOrderDetail(Long userId, String orderNo) {
        return attractionOrderMapper.selectOne(new LambdaQueryWrapper<AttractionOrder>()
                .eq(AttractionOrder::getUserId, userId)
                .eq(AttractionOrder::getOrderNo, orderNo)
                .eq(AttractionOrder::getDeleted, 0));
    }
}
