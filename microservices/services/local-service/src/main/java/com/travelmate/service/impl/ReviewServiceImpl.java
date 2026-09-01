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
        if (review == null || review.getUserId() == null) {
            throw new RuntimeException("用户信息无效");
        }
        if (review.getTargetId() == null || review.getTargetType() == null
                || (review.getTargetType() != 0 && review.getTargetType() != 1)) {
            throw new RuntimeException("评价目标无效");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new RuntimeException("评分必须在1到5之间");
        }
        String content = review.getContent() == null ? "" : review.getContent().trim();
        if (content.isEmpty()) {
            throw new RuntimeException("评价内容不能为空");
        }
        if (content.length() > 2000) {
            throw new RuntimeException("评价内容不能超过2000字");
        }
        review.setContent(content);
        if (review.getOrderId() != null && count(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, review.getUserId())
                .eq(Review::getOrderId, review.getOrderId())
                .eq(Review::getDeleted, 0)) > 0) {
            throw new RuntimeException("该订单已评价");
        }
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
