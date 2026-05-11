package com.travelmate.service;

import java.util.Map;

public interface LikeService {

    Map<String, Object> toggleLike(Long userId, Map<String, Object> body);

    int likeStatus(Long userId, Long targetId, Integer targetType);
}
