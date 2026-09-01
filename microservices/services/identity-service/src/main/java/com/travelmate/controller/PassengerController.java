package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.Passenger;
import com.travelmate.service.PassengerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passenger")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/list")
    public Result<List<Passenger>> getList() {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        return Result.success(passengerService.getPassengerList(userId));
    }

    @PostMapping("/add")
    public Result<String> addPassenger(@RequestBody Passenger passenger) {
        if (!StringUtils.hasText(passenger.getName()) || !StringUtils.hasText(passenger.getIdCard())) {
            return Result.error("名称和证件号不能为空");
        }
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        passenger.setUserId(userId);
        return passengerService.addPassenger(passenger) ? Result.success("添加成功") : Result.error("添加失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> deletePassenger(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        return passengerService.deletePassenger(id, userId) ? Result.success("删除成功") : Result.error("删除失败");
    }

    private Long getCurrentUserId() {
        return userContext.getCurrentUserIdOrNull();
    }
}
