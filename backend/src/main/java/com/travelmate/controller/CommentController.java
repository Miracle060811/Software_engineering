package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Comment;
import com.travelmate.entity.Post;
import com.travelmate.mapper.CommentMapper;
import com.travelmate.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * GET /api/comment/list?postId= - 获取评论列表（树形结构）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listComments(@RequestParam Long postId) {
        // 查所有一级评论
        LambdaQueryWrapper<Comment> rootQuery = new LambdaQueryWrapper<>();
        rootQuery.eq(Comment::getPostId, postId)
                .isNull(Comment::getParentId)
                .orderByAsc(Comment::getCreateTime);
        List<Comment> rootComments = commentMapper.selectList(rootQuery);

        // 查所有子评论
        LambdaQueryWrapper<Comment> subQuery = new LambdaQueryWrapper<>();
        subQuery.eq(Comment::getPostId, postId)
                .isNotNull(Comment::getParentId)
                .orderByAsc(Comment::getCreateTime);
        List<Comment> subComments = commentMapper.selectList(subQuery);

        // 构建树形结构
        Map<Long, List<Comment>> subMap = subComments.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment root : rootComments) {
            Map<String, Object> node = commentToMap(root);
            List<Comment> children = subMap.getOrDefault(root.getId(), List.of());
            node.put("children", children.stream().map(this::commentToMap).collect(Collectors.toList()));
            result.add(node);
        }
        return Result.success(result);
    }

    /**
     * POST /api/comment/add - 发表评论
     * Body: { postId, content, parentId(可选) }
     */
    @PostMapping("/add")
    public Result<Comment> addComment(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();

        Long postId = Long.parseLong(body.get("postId").toString());
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null
                ? Long.parseLong(body.get("parentId").toString())
                : null;

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setDeleted(0);
        comment.setCreateTime(LocalDateTime.now());

        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent != null) {
                comment.setReplyUserId(parent.getUserId());
            }
        }

        commentMapper.insert(comment);

        // 更新游记评论数
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, postId).setSql("comment_count = comment_count + 1");
        postMapper.update(null, upd);

        return Result.success(comment);
    }

    /**
     * DELETE /api/comment/{id} - 删除评论
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            return Result.error("无权删除他人评论");
        }
        commentMapper.deleteById(id);

        // 减少游记评论数
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, comment.getPostId()).setSql("comment_count = GREATEST(comment_count - 1, 0)");
        postMapper.update(null, upd);

        return Result.success();
    }

    // ======================== 工具方法 ========================

    private Map<String, Object> commentToMap(Comment c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("postId", c.getPostId());
        map.put("userId", c.getUserId());
        map.put("parentId", c.getParentId());
        map.put("replyUserId", c.getReplyUserId());
        map.put("content", c.getContent());
        map.put("likeCount", c.getLikeCount());
        map.put("createTime", c.getCreateTime());
        return map;
    }

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
