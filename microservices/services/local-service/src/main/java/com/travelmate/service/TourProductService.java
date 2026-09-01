package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.TourProduct;
import java.util.List;

public interface TourProductService extends IService<TourProduct> {
    List<TourProduct> listByType(Integer tourType);
}
