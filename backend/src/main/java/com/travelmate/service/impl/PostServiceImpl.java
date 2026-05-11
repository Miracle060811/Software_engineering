package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.entity.Post;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.PostService;
import com.travelmate.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Override
    public List<Map<String, Object>> listPosts(int page, int size) {
        int offset = (page - 1) * size;
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
        post.setStatus(0);
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
        wrapper.eq(Post::getUserId, userId).orderByDesc(Post::getCreateTime);
        return postMapper.selectList(wrapper);
    }
}
