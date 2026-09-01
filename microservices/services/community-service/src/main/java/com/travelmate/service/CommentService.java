package com.travelmate.service;

import java.util.List;
import java.util.Map;

public interface CommentService {

    List<Map<String, Object>> listComments(Long postId);

    Map<String, Object> addComment(Map<String, Object> body, Long userId);

    void deleteComment(Long id, Long userId);
}
