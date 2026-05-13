package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.Post;
import com.travelmate.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import com.travelmate.annotation.RateLimiter;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.listPosts(page, size));
    }

    @GetMapping("/{id}")
    public Result<Post> getPost(@PathVariable Long id) {
        return Result.success(postService.getPostDetail(id));
    }

    @RateLimiter(maxRequests = 2, timeWindowSeconds = 1)
    @PostMapping("/create")
    public Result<Post> createPost(@RequestBody Map<String, Object> body) {
        return Result.success(postService.createPost(body, userContext.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id, userContext.getCurrentUserId());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<Post>> myPosts() {
        return Result.success(postService.myPosts(userContext.getCurrentUserId()));
    }

    @GetMapping("/following")
    public Result<List<Map<String, Object>>> followingPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.getFollowingPosts(userContext.getCurrentUserId(), page, size));
    }
}
