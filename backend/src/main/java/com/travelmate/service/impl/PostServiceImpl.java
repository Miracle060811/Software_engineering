package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.PaginationSupport;
import com.travelmate.entity.Follow;
import com.travelmate.entity.Post;
import com.travelmate.mapper.FollowMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PostServiceImpl implements PostService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Map<String, Object>> listPosts(int page, int size, String keyword) {
        PaginationSupport.Page pagination = PaginationSupport.normalize(page, size, MAX_PAGE_SIZE);
        // 只返回已发布且公开的帖子
        return postMapper.selectPostsWithUser(
                pagination.offset(), pagination.size(), normalizeKeyword(keyword));
    }

    @Override
    public Post getPostDetail(Long id, Long currentUserId) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("游记不存在");
        }
        boolean isAuthor = currentUserId != null && Objects.equals(post.getUserId(), currentUserId);
        if (!isAuthor && !Objects.equals(post.getStatus(), 1)) {
            throw new RuntimeException("游记正在审核或已下架");
        }
        if (!isAuthor && Objects.equals(post.getVisibility(), 2)) {
            throw new RuntimeException("该游记为私密内容");
        }
        if (!isAuthor && Objects.equals(post.getVisibility(), 1) && !isFollowing(currentUserId, post.getUserId())) {
            throw new RuntimeException("关注作者后可查看该游记");
        }
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, id).setSql("view_count = view_count + 1");
        postMapper.update(null, upd);
        fillAuthor(post);
        return post;
    }

    private void fillAuthor(Post post) {
        if (post == null || post.getUserId() == null) {
            return;
        }
        User author = userMapper.selectById(post.getUserId());
        if (author == null) {
            return;
        }
        post.setAuthorUsername(author.getUsername());
        post.setAuthorNickname(author.getNickname());
        post.setAuthorAvatar(author.getAvatar());
    }

    private boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return false;
        }
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, followerId).eq(Follow::getFolloweeId, followeeId);
        return followMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Post createPost(Map<String, Object> body, Long userId) {
        boolean draft = isDraft(body);
        String title = normalizePostText(body.get("title"), draft, "未命名草稿", "标题不能为空", 100, "标题不能超过100个字符");
        String content = normalizePostText(body.get("content"), draft, "", "内容不能为空", 10000, "内容不能超过10000个字符");
        String images = limitString(body.get("images"), 2000, "图片地址过长");
        String destination = limitString(body.get("destination"), 100, "目的地不能超过100个字符");
        String tags = limitString(body.get("tags"), 500, "标签不能超过500个字符");

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setImages(images);
        post.setDestination(destination);
        post.setTags(tags);
        // 非草稿进入 AI 审核队列，由定时任务按限流窗口处理。
        post.setStatus(draft ? 3 : 0);
        post.setRejectReason(null);
        // 可见范围: 0=公开, 1=仅关注者, 2=私密 (默认公开)
        Integer visibility = body.get("visibility") != null ? Integer.parseInt(body.get("visibility").toString()) : 0;
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
    public Post updatePost(Long id, Map<String, Object> body, Long userId) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("游记不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权编辑他人游记");
        }

        boolean draft = isDraft(body);
        String title = normalizePostText(body.get("title"), draft, "未命名草稿", "标题不能为空", 100, "标题不能超过100个字符");
        String content = normalizePostText(body.get("content"), draft, "", "内容不能为空", 10000, "内容不能超过10000个字符");
        String images = limitString(body.get("images"), 2000, "图片地址过长");
        String destination = limitString(body.get("destination"), 100, "目的地不能超过100个字符");
        String tags = limitString(body.get("tags"), 500, "标签不能超过500个字符");

        post.setTitle(title);
        post.setContent(content);
        post.setImages(images);
        post.setDestination(destination);
        post.setTags(tags);
        post.setStatus(draft ? 3 : 0);
        Integer visibility = body.get("visibility") != null ? Integer.parseInt(body.get("visibility").toString()) : 0;
        post.setVisibility(visibility);
        post.setRejectReason(null);
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
        fillAuthor(post);
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
                .orderByDesc(Post::getCreateTime);
        List<Post> posts = postMapper.selectList(wrapper);
        posts.forEach(this::fillAuthor);
        return posts;
    }

    @Override
    public List<Map<String, Object>> getFollowingPosts(Long userId, int page, int size, String keyword) {
        // 获取用户关注的人列表
        LambdaQueryWrapper<Follow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(Follow::getFollowerId, userId);
        List<Follow> follows = followMapper.selectList(followWrapper);
        if (follows.isEmpty()) return Collections.emptyList();

        List<Long> followeeIds = follows.stream().map(Follow::getFolloweeId).toList();

        PaginationSupport.Page pagination = PaginationSupport.normalize(page, size, MAX_PAGE_SIZE);
        return postMapper.selectPostsByUserIds(
                followeeIds, pagination.offset(), pagination.size(), normalizeKeyword(keyword));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return "%" + keyword.trim() + "%";
    }

    private String limitString(Object value, int maxLength, String errorMessage) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.length() > maxLength) {
            throw new RuntimeException(errorMessage);
        }
        return text;
    }

    private boolean isDraft(Map<String, Object> body) {
        return body.get("status") != null && Integer.parseInt(body.get("status").toString()) == 3;
    }

    private String normalizePostText(Object value, boolean draft, String draftDefault, String emptyMessage, int maxLength, String lengthMessage) {
        String text = value == null ? "" : value.toString().trim();
        if (text.isEmpty()) {
            if (draft) {
                return draftDefault;
            }
            throw new RuntimeException(emptyMessage);
        }
        if (text.length() > maxLength) {
            throw new RuntimeException(lengthMessage);
        }
        return text;
    }

}
