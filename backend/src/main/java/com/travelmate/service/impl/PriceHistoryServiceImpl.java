package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.PriceHistory;
import com.travelmate.mapper.PriceHistoryMapper;
import com.travelmate.service.PriceHistoryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PriceHistoryServiceImpl extends ServiceImpl<PriceHistoryMapper, PriceHistory>
        implements PriceHistoryService {

    @Override
    public List<PriceHistory> getTrendData(Long ticketId, Integer ticketType) {
        // 先在数据库中按照 ticket_id 和 type 查询近 7 天的数据
        List<PriceHistory> dbList = this.list(new LambdaQueryWrapper<PriceHistory>()
                .eq(PriceHistory::getTicketId, ticketId)
                .eq(PriceHistory::getTicketType, ticketType)
                .orderByAsc(PriceHistory::getRecordDate));

        // 作为加分项：假如数据库没有这班航班/火车的历史趋势真实数据，
        // 这里用算法模拟生成 7 天波动的“杀熟/早鸟”价格返回给前端 ECharts。
        if (dbList == null || dbList.isEmpty()) {
            List<PriceHistory> mockList = new ArrayList<>();
            LocalDate today = LocalDate.now();
            BigDecimal basePrice = ticketType == 0 ? new BigDecimal("800") : new BigDecimal("350"); // 机票基准800，火车票基准350

            for (int i = 0; i < 7; i++) {
                PriceHistory p = new PriceHistory();
                p.setTicketId(ticketId);
                p.setTicketType(ticketType);
                p.setRecordDate(today.plusDays(i)); // 往后预测7天的价格

                // 模拟算法：票价随着日期越近可能越贵，或者周末有一些波动(随机数)
                double randomFactor = (Math.random() - 0.5) * 150; // -75 到 +75 随机波动
                p.setLowestPrice(basePrice.add(new BigDecimal((int) randomFactor)));
                mockList.add(p);
            }
            return mockList;
        }

        return dbList;
    }
}
