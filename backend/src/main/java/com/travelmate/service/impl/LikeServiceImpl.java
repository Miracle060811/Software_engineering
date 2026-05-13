package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Like;
import com.travelmate.entity.Post;
import com.travelmate.mapper.LikeMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private PostMapper postMapper;

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long userId, Map<String, Object> body) {
        Long targetId = Long.valueOf(body.get("targetId").toString());
        Integer targetType = Integer.valueOf(body.get("targetType").toString());

        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetId, targetId)
                .eq(Like::getTargetType, targetType);
        Like existing = likeMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            result.put("liked", false);
            // 回退点赞/收藏计数
            if (targetType == 0) {
                decrementPostLikeCount(targetId);
            } else if (targetType == 2) {
                decrementPostCollectCount(targetId);
            }
        } else {
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setTargetType(targetType);
            likeMapper.insert(like);
            result.put("liked", true);
            if (targetType == 0) {
                incrementPostLikeCount(targetId);
            } else if (targetType == 2) {
                incrementPostCollectCount(targetId);
            }
        }

        return result;
    }

    @Override
    public int likeStatus(Long userId, Long targetId, Integer targetType) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetId, targetId)
                .eq(Like::getTargetType, targetType);
        return likeMapper.selectCount(wrapper) > 0 ? 1 : 0;
    }

    private void incrementPostLikeCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount((post.getLikeCount() != null ? post.getLikeCount() : 0) + 1);
            postMapper.updateById(post);
        }
    }

    private void decrementPostLikeCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null && post.getLikeCount() != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
        }
    }

    private void incrementPostCollectCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setCollectCount((post.getCollectCount() != null ? post.getCollectCount() : 0) + 1);
            postMapper.updateById(post);
        }
    }

    @Override
    public List<Map<String, Object>> getMyCollects(Long userId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetType, 2)
                .orderByDesc(Like::getCreateTime);
        List<Like> likes = likeMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Like like : likes) {
            Post post = postMapper.selectById(like.getTargetId());
            if (post != null && post.getStatus() == 1) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", post.getId());
                m.put("title", post.getTitle());
                m.put("content", post.getContent());
                m.put("images", post.getImages());
                m.put("destination", post.getDestination());
                m.put("tags", post.getTags());
                m.put("likeCount", post.getLikeCount());
                m.put("commentCount", post.getCommentCount());
                m.put("viewCount", post.getViewCount());
                m.put("createTime", post.getCreateTime());
                result.add(m);
            }
        }
        return result;
    }

    private void decrementPostCollectCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null && post.getCollectCount() != null && post.getCollectCount() > 0) {
            post.setCollectCount(post.getCollectCount() - 1);
            postMapper.updateById(post);
        }
    }
}
