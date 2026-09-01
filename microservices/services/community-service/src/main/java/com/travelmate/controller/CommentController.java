package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listComments(@RequestParam Long postId) {
        return Result.success(commentService.listComments(postId));
    }

    @PostMapping("/add")
    public Result<Map<String, Object>> addComment(@RequestBody Map<String, Object> body) {
        return Result.success(commentService.addComment(body, userContext.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id, userContext.getCurrentUserId());
        return Result.success();
    }
}
