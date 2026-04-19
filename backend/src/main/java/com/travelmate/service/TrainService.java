package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Train;

import java.util.List;

public interface TrainService extends IService<Train> {

    /**
     * 查询火车票列表
     * 
     * @param depStation 出发站
     * @param arrStation 到达站
     * @param depDate    出发日期
     */
    List<Train> searchTrains(String depStation, String arrStation, String depDate);

    /**
     * 智能中转推荐 (例如: 出发地查不到直达, 找途经第三站拼接的两段列车)
     */
    List<java.util.List<Train>> getTransferPlan(String depStation, String arrStation, String depDate);
}
