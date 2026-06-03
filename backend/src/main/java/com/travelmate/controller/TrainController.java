package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.dto.TrainLiveSyncStatus;
import com.travelmate.entity.Train;
import com.travelmate.service.TrainLiveSyncService;
import com.travelmate.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成员A负责: 火车票查寻模块
 */
@RestController
@RequestMapping("/api/train")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @Autowired
    private TrainLiveSyncService trainLiveSyncService;

    /**
     * @param depStation 出发点 (如 北京南)
     * @param arrStation 到达点 (如 上海虹桥)
     * @param date       出发日期 (如: 2024-05-01)
     * @return 列车车次列表
     */
    @GetMapping("/search")
    public Result<List<Train>> search(
            @RequestParam(required = false) String depStation,
            @RequestParam(required = false) String arrStation,
            @RequestParam(required = false) String date) {

        List<Train> list = trainService.searchTrains(depStation, arrStation, date);
        return Result.success(list);
    }

    @GetMapping("/live-sync-status")
    public Result<TrainLiveSyncStatus> liveSyncStatus() {
        return Result.success(trainLiveSyncService.getLastStatus());
    }

    /**
     * 获取列车详情
     */
    @GetMapping("/{id}")
    public Result<Train> getDetail(@PathVariable Long id) {
        Train train = trainService.getById(id);
        if (train == null || train.getStatus() == 0) {
            return Result.error("车次不存在或已停运");
        }
        return Result.success(train);
    }

    /**
     * 加分项: 火车票智能中转路线推荐算法
     */
    @GetMapping("/transfer")
    public Result<List<java.util.List<Train>>> getTransfer(
            @RequestParam String depStation,
            @RequestParam String arrStation,
            @RequestParam String date) {

        return Result.success(trainService.getTransferPlan(depStation, arrStation, date));
    }
}
