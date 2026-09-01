package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Destination;
import com.travelmate.mapper.DestinationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    @Autowired
    private DestinationMapper destinationMapper;

    @GetMapping
    public Result<List<Destination>> listDestinations() {
        return Result.success(destinationMapper.selectList(new LambdaQueryWrapper<Destination>()
                .eq(Destination::getStatus, 1)
                .orderByAsc(Destination::getSortOrder)
                .orderByDesc(Destination::getId)));
    }

    @GetMapping("/{slug}")
    public Result<Destination> getDestination(@PathVariable String slug) {
        Destination destination = destinationMapper.selectOne(new LambdaQueryWrapper<Destination>()
                .eq(Destination::getSlug, slug)
                .eq(Destination::getStatus, 1)
                .last("LIMIT 1"));
        if (destination == null) {
            return Result.error("城市资料不存在");
        }
        return Result.success(destination);
    }
}
