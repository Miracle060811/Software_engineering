package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.entity.Follow;
import com.travelmate.entity.Post;
import com.travelmate.mapper.FollowMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.PostService;
import com.travelmate.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Override
    public List<Map<String, Object>> listPosts(int page, int size) {
        int offset = (page - 1) * size;
        // 只返回已发布且公开的帖子
        return postMapper.selectPostsWithUser(offset, size);
    }

    @Override
    public Post getPostDetail(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("游记不存在");
        }
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, id).setSql("view_count = view_count + 1");
        postMapper.update(null, upd);
        return post;
    }

    @Override
    public Post createPost(Map<String, Object> body, Long userId) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");

        if (sensitiveWordService.containsSensitiveWord(title)) {
            throw new RuntimeException("标题包含敏感词，请修改后发布");
        }
        if (sensitiveWordService.containsSensitiveWord(content)) {
            throw new RuntimeException("内容包含敏感词，请修改后发布");
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setImages((String) body.get("images"));
        post.setDestination((String) body.get("destination"));
        post.setTags((String) body.get("tags"));
        // 支持指定 status: 0=审核中, 1=直接发布, 3=草稿
        Integer reqStatus = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : 1;
        post.setStatus(reqStatus == 3 ? 3 : 1);
        // 可见范围: 0=公开, 1=仅关注者, 2=私密 (默认公开)
        Integer visibility = body.get("visibility") != null ? Integer.valueOf(body.get("visibility").toString()) : 0;
        post.setVisibility(visibility);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setViewCount(0);
        post.setDeleted(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());

        postMapper.insert(post);
        return post;
    }

    @Override
    public void deletePost(Long id, Long userId) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("游记不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人游记");
        }
        postMapper.deleteById(id);
    }

    @Override
    public List<Post> myPosts(Long userId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId)
                .ne(Post::getStatus, 3)  // 排除草稿
                .orderByDesc(Post::getCreateTime);
        return postMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> getFollowingPosts(Long userId, int page, int size) {
        // 获取用户关注的人列表
        LambdaQueryWrapper<Follow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(Follow::getFollowerId, userId);
        List<Follow> follows = followMapper.selectList(followWrapper);
        if (follows.isEmpty()) return Collections.emptyList();

        List<Long> followeeIds = follows.stream().map(Follow::getFolloweeId).toList();

        int offset = (page - 1) * size;
        return postMapper.selectPostsByUserIds(followeeIds, offset, size);
    }
}
