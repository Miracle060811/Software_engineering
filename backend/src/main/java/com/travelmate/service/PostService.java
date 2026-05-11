package com.travelmate.service;

import com.travelmate.entity.Post;

import java.util.List;
import java.util.Map;

public interface PostService {

    List<Map<String, Object>> listPosts(int page, int size);

    Post getPostDetail(Long id);

    Post createPost(Map<String, Object> body, Long userId);

    void deletePost(Long id, Long userId);

    List<Post> myPosts(Long userId);
}
