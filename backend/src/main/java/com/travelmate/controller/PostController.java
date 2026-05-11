package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Post;
import com.travelmate.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * GET /api/post/list?page=1&size=10 - 瀑布流游记列表（含用户信息）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> posts = postMapper.selectPostsWithUser(offset, size);
        return Result.success(posts);
    }

    /**
     * GET /api/post/{id} - 游记详情
     */
    @GetMapping("/{id}")
    public Result<Post> getPost(@PathVariable Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return Result.error("游记不存在");
        }
        // 增加浏览量
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, id)
                .setSql("view_count = view_count + 1");
        postMapper.update(null, upd);
        return Result.success(post);
    }

    /**
     * POST /api/post/create - 发布游记（需认证）
     */
    @PostMapping("/create")
    public Result<Post> createPost(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle((String) body.get("title"));
        post.setContent((String) body.get("content"));
        post.setImages((String) body.get("images"));
        post.setDestination((String) body.get("destination"));
        post.setTags((String) body.get("tags"));
        post.setStatus(1); // 默认直接发布，管理员可下架
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setViewCount(0);
        post.setDeleted(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());

        postMapper.insert(post);
        return Result.success(post);
    }

    /**
     * DELETE /api/post/{id} - 删除自己的游记
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Post post = postMapper.selectById(id);
        if (post == null) {
            return Result.error("游记不存在");
        }
        if (!post.getUserId().equals(userId)) {
            return Result.error("无权删除他人游记");
        }
        postMapper.deleteById(id);
        return Result.success();
    }

    /**
     * GET /api/post/my - 我发布的游记
     */
    @GetMapping("/my")
    public Result<List<Post>> myPosts() {
        Long userId = getCurrentUserId();
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreateTime);
        return Result.success(postMapper.selectList(wrapper));
    }

    // ======================== 工具方法 ========================

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getId();
    }
}
