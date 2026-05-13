package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.entity.TourProduct;
import com.travelmate.service.TourProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tour")
public class TourProductController {

    @Autowired
    private TourProductService tourProductService;

    @GetMapping("/list")
    public Result<List<TourProduct>> list(@RequestParam(defaultValue = "0") Integer type) {
        return Result.success(tourProductService.listByType(type));
    }
}
