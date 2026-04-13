package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.PriceHistory;

import java.util.List;

public interface PriceHistoryService extends IService<PriceHistory> {
    /**
     * 获取某一班次在前后几天的价格趋势折线图数据 (用于前端渲染 ECharts)
     * 如果数据库没数据，系统会自动生成一段波动的平滑伪造数据补充。
     */
    List<PriceHistory> getTrendData(Long ticketId, Integer ticketType);
}
