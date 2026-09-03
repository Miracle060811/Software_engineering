package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.dto.TourBookingCreateDTO;
import com.travelmate.entity.TourOrder;
import com.travelmate.entity.TourProduct;
import java.util.List;

public interface TourProductService extends IService<TourProduct> {
    List<TourProduct> listByType(Integer tourType);

    TourOrder createBooking(Long userId, TourBookingCreateDTO request);

    TourOrder findIdempotentBooking(Long userId, TourBookingCreateDTO request);

    List<TourOrder> listUserOrders(Long userId);

    TourOrder getUserOrder(Long userId, String orderNo);
}
