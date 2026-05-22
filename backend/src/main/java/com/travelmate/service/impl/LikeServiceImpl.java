package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
        Long targetId = parseLong(body.get("targetId"), "目标ID无效");
        Integer targetType = parseTargetType(body.get("targetType"));
        if (targetType != 0 && targetType != 1 && targetType != 2) {
            throw new RuntimeException("不支持的操作类型");
        }

        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetId, targetId)
                .eq(Like::getTargetType, targetType);
        Like existing = likeMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        boolean active;
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            active = false;
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
            active = true;
            if (targetType == 0) {
                incrementPostLikeCount(targetId);
            } else if (targetType == 2) {
                incrementPostCollectCount(targetId);
            }
        }

        Post post = targetType == 0 || targetType == 2 ? postMapper.selectById(targetId) : null;
        result.put("liked", active);
        result.put("collected", active);
        if (targetType == 0) {
            result.put("count", post == null ? 0 : Optional.ofNullable(post.getLikeCount()).orElse(0));
        } else if (targetType == 2) {
            result.put("count", post == null ? 0 : Optional.ofNullable(post.getCollectCount()).orElse(0));
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
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Post::getId, postId).setSql("like_count = COALESCE(like_count, 0) + 1");
        postMapper.update(null, wrapper);
    }

    private void decrementPostLikeCount(Long postId) {
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Post::getId, postId).setSql("like_count = GREATEST(COALESCE(like_count, 0) - 1, 0)");
        postMapper.update(null, wrapper);
    }

    private void incrementPostCollectCount(Long postId) {
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Post::getId, postId).setSql("collect_count = COALESCE(collect_count, 0) + 1");
        postMapper.update(null, wrapper);
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
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Post::getId, postId).setSql("collect_count = GREATEST(COALESCE(collect_count, 0) - 1, 0)");
        postMapper.update(null, wrapper);
    }

    private Long parseLong(Object value, String message) {
        if (value == null) {
            throw new RuntimeException(message);
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException(message);
        }
    }

    private Integer parseTargetType(Object value) {
        if (value == null) {
            throw new RuntimeException("操作类型不能为空");
        }
        String raw = value.toString().trim();
        return switch (raw.toLowerCase()) {
            case "post", "like", "post_like" -> 0;
            case "comment", "comment_like" -> 1;
            case "collect", "favorite", "post_collect" -> 2;
            default -> {
                try {
                    yield Integer.valueOf(raw);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("操作类型无效");
                }
            }
        };
    }
}
