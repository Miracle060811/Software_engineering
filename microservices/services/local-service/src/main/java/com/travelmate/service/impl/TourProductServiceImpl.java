package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.TourProduct;
import com.travelmate.mapper.TourProductMapper;
import com.travelmate.service.TourProductService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TourProductServiceImpl extends ServiceImpl<TourProductMapper, TourProduct>
        implements TourProductService {

    @Override
    public List<TourProduct> listByType(Integer tourType) {
        return list(new LambdaQueryWrapper<TourProduct>()
                .eq(TourProduct::getTourType, tourType)
                .eq(TourProduct::getStatus, 1)
                .orderByDesc(TourProduct::getCreateTime));
    }
}
