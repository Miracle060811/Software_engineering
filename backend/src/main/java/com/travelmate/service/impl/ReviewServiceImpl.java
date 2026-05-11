package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Review;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.service.ReviewService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review>
        implements ReviewService {

    @Override
    public void addReview(Review review) {
        if (review.getCreateTime() == null) {
            review.setCreateTime(LocalDateTime.now());
        }
        save(review);
    }

    @Override
    public List<Review> getReviews(Long targetId, Integer targetType) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, targetId)
                .eq(Review::getTargetType, targetType)
                .orderByDesc(Review::getCreateTime);
        return list(wrapper);
    }
}
