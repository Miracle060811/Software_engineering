package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.entity.Passenger;
import com.travelmate.service.PassengerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成员A负责: 常用旅客(乘车人)管理
 */
@RestController
@RequestMapping("/api/passenger")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    /**
     * 获取当前用户的常用旅客列表
     * 临时模拟：使用 userId=1 代替当前登录用户
     */
    @GetMapping("/list")
    public Result<List<Passenger>> getList() {
        Long currentUserId = 1L; // TODO: 应该从登录鉴权拦截器(JWT)中获取
        List<Passenger> list = passengerService.getPassengerList(currentUserId);
        return Result.success(list);
    }

    /**
     * 添加新的常用旅客
     */
    @PostMapping("/add")
    public Result<String> addPassenger(@RequestBody Passenger passenger) {
        if (!StringUtils.hasText(passenger.getName()) || !StringUtils.hasText(passenger.getIdCard())) {
            return Result.error("名称和证件号不能为空");
        }

        Long currentUserId = 1L; // TODO: 后期替换为JWT token获取
        passenger.setUserId(currentUserId);

        boolean success = passengerService.addPassenger(passenger);
        return success ? Result.success("添加成功") : Result.error("添加失败");
    }

    /**
     * 删除常用旅客
     * 
     * @param id 旅客的主键id
     */
    @DeleteMapping("/{id}")
    public Result<String> deletePassenger(@PathVariable Long id) {
        Long currentUserId = 1L; // TODO: 后期替换为JWT token获取
        boolean success = passengerService.deletePassenger(id, currentUserId);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
