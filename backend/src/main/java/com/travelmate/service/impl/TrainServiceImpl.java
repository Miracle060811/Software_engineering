package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Train;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.TrainService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TrainServiceImpl extends ServiceImpl<TrainMapper, Train> implements TrainService {

    @Override
    public List<Train> searchTrains(String depStation, String arrStation, String depDate) {
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Train::getStatus, 1);

        // 只要有一等座或者二等座其中之一有票就可以展示
        wrapper.and(w -> w.gt(Train::getFirstClassSeats, 0)
                .or()
                .gt(Train::getSecondClassSeats, 0));

        if (StringUtils.hasText(depStation)) {
            wrapper.eq(Train::getDepartureStation, depStation);
        }
        if (StringUtils.hasText(arrStation)) {
            wrapper.eq(Train::getArrivalStation, arrStation);
        }
        if (StringUtils.hasText(depDate)) {
            wrapper.likeRight(Train::getDepartureTime, depDate);
        }

        wrapper.orderByAsc(Train::getDepartureTime);
        return list(wrapper);
    }

    /**
     * 算法难点加分项 - "智能中转方案拼接"
     * 如果直达票没票，或者干脆没有班次，给用户一条可以拆分成两段买的车票。
     */
    @Override
    public List<java.util.List<Train>> getTransferPlan(String depStation, String arrStation, String depDate) {
        List<java.util.List<Train>> result = new java.util.ArrayList<>();

        // 1. 获取所有"从出发地"出去的第一程车
        List<Train> firstLegMatches = this.searchTrains(depStation, null, depDate);

        // 2. 根据这些第一程的到达地去找是否有去"最终目的地"的第二程
        // 条件：接驳等待时间合理比如 >= 30分钟且 <= 5小时。
        for (Train leg1 : firstLegMatches) {
            String midStation = leg1.getArrivalStation();

            // 为了防止无限长或者死循环只找发车时间 > 第一段到达时间 30分钟 的车 (给乘车换乘余量)
            List<Train> leg2Matches = this.list(new LambdaQueryWrapper<Train>()
                    .eq(Train::getDepartureStation, midStation)
                    .eq(Train::getArrivalStation, arrStation)
                    .gt(Train::getDepartureTime, leg1.getArrivalTime().plusMinutes(30))
                    .lt(Train::getDepartureTime, leg1.getArrivalTime().plusHours(5))
                    // 同理保证余票
                    .and(w -> w.gt(Train::getFirstClassSeats, 0).or().gt(Train::getSecondClassSeats, 0)));

            if (leg2Matches != null && !leg2Matches.isEmpty()) {
                // 每抓到满足条件的一段，就拼成一个List装进方案库
                for (Train leg2 : leg2Matches) {
                    List<Train> plan = new java.util.ArrayList<>();
                    plan.add(leg1);
                    plan.add(leg2);
                    result.add(plan);
                }
            }
        }
        return result;
    }
}
