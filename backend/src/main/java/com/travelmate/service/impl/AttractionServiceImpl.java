package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Attraction;
import com.travelmate.mapper.AttractionMapper;
import com.travelmate.service.AttractionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class AttractionServiceImpl extends ServiceImpl<AttractionMapper, Attraction>
        implements AttractionService {

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
    public String buyTicket(Long userId, Long attractionId, Integer count,
            String guestName, String guestPhone) {
        if (count == null || count <= 0) {
            throw new RuntimeException("购买数量不合法");
        }

        // 查询景点信息
        Attraction attraction = getById(attractionId);
        if (attraction == null || attraction.getStatus() != 1) {
            throw new RuntimeException("景点不存在或已下线");
        }

        // 乐观锁扣减票数（支持批量购买）
        int updated = baseMapper.deductTicket(attractionId, count);
        if (updated == 0) {
            throw new RuntimeException("门票库存不足，购票失败");
        }

        // 生成景点订单号 (前缀 AT = Attraction Ticket)
        String orderNo = "AT" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        System.out.println("====== [Attraction] 购票成功，订单号: " + orderNo
                + "，景点: " + attraction.getName()
                + "，数量: " + count + " ======");

        return orderNo;
    }
}
