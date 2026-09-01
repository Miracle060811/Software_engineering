package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.entity.Follow;
import com.travelmate.mapper.FollowMapper;
import com.travelmate.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public int toggleFollow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            throw new RuntimeException("用户信息无效");
        }
        if (followerId.equals(followeeId)) {
            throw new RuntimeException("不能关注自己");
        }
        User target = userMapper.selectById(followeeId);
        if (target == null || Integer.valueOf(0).equals(target.getStatus())
                || Integer.valueOf(1).equals(target.getDeleted())) {
            throw new RuntimeException("被关注用户不存在");
        }

        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, followeeId);
        Follow existing = followMapper.selectOne(wrapper);

        if (existing != null) {
            followMapper.deleteById(existing.getId());
            return 0; // 已取消关注
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            followMapper.insert(follow);
            return 1; // 已关注
        }
    }

    @Override
    public List<User> fans(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFolloweeId, userId);
        List<Follow> follows = followMapper.selectList(wrapper);
        List<Long> followerIds = follows.stream().map(Follow::getFollowerId).collect(Collectors.toList());
        if (followerIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(followerIds);
    }

    @Override
    public List<User> following(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId);
        List<Follow> follows = followMapper.selectList(wrapper);
        List<Long> followeeIds = follows.stream().map(Follow::getFolloweeId).collect(Collectors.toList());
        if (followeeIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(followeeIds);
    }

    @Override
    public int followStatus(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return 0;
        }
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, followeeId);
        return followMapper.selectCount(wrapper) > 0 ? 1 : 0;
    }
}
