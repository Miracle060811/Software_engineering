package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.entity.PriceHistory;
import com.travelmate.service.PriceHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成员A负责: 历史价格日历波动的折线图接口 (对接 Echarts 的重点加分项)
 */
@CrossOrigin
@RestController
@RequestMapping("/api/price")
public class PriceHistoryController {

    @Autowired
    private PriceHistoryService priceHistoryService;

    /**
     * 获取航班/火车的未来 7 天最低波动趋势 (如果有真实历史则读取库, 否则用曲线预测假数据)
     * 
     * @param type 0:机票; 1:火车票
     * @param id   班次ID
     */
    @GetMapping("/trend")
    public Result<List<PriceHistory>> getTrend(
            @RequestParam Integer type,
            @RequestParam Long id) {
        return Result.success(priceHistoryService.getTrendData(id, type));
    }
}
