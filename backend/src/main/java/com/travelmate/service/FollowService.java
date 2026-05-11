package com.travelmate.service;

import com.travelmate.backend.entity.User;

import java.util.List;

public interface FollowService {

    int toggleFollow(Long followerId, Long followeeId);

    List<User> fans(Long userId);

    List<User> following(Long userId);

    int followStatus(Long followerId, Long followeeId);
}
