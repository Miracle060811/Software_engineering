package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Review;

import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService extends IService<Review> {

    /**
     * 添加评价
     *
     * @param review 评价实体
     */
    void addReview(Review review);

    /**
     * 获取评价列表
     *
     * @param targetId   目标ID (酒店ID 或 景点ID)
     * @param targetType 目标类型 (0-酒店, 1-景点)
     * @return 评价列表
     */
    List<Review> getReviews(Long targetId, Integer targetType);
}
