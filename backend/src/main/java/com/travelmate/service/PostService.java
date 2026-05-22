package com.travelmate.service;

import com.travelmate.entity.Post;

import java.util.List;
import java.util.Map;

public interface PostService {

    List<Map<String, Object>> listPosts(int page, int size, String keyword);

    Post getPostDetail(Long id, Long currentUserId);

    Post createPost(Map<String, Object> body, Long userId);

    Post updatePost(Long id, Map<String, Object> body, Long userId);

    void deletePost(Long id, Long userId);

    List<Post> myPosts(Long userId);

    List<Map<String, Object>> getFollowingPosts(Long userId, int page, int size, String keyword);
}
