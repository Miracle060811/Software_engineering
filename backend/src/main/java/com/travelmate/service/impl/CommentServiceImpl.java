package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.entity.Comment;
import com.travelmate.entity.Post;
import com.travelmate.mapper.CommentMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.CommentService;
import com.travelmate.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Override
    public List<Map<String, Object>> listComments(Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId).orderByAsc(Comment::getCreateTime);
        List<Comment> all = commentMapper.selectList(wrapper);
        Map<Long, User> users = loadUsers(all);

        Map<Long, Map<String, Object>> map = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (Comment c : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", c.getId());
            node.put("postId", c.getPostId());
            node.put("userId", c.getUserId());
            node.put("parentId", c.getParentId());
            node.put("replyUserId", c.getReplyUserId());
            node.put("content", c.getContent());
            node.put("likeCount", c.getLikeCount());
            node.put("createTime", c.getCreateTime());
            fillAuthor(node, users.get(c.getUserId()));
            fillReplyUser(node, users.get(c.getReplyUserId()));
            node.put("children", new ArrayList<>());
            map.put(c.getId(), node);
        }

        for (Comment c : all) {
            Map<String, Object> node = map.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(node);
            } else {
                Map<String, Object> parent = map.get(c.getParentId());
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                }
            }
        }

        return roots;
    }

    private Map<Long, User> loadUsers(List<Comment> comments) {
        Set<Long> userIds = new HashSet<>();
        for (Comment comment : comments) {
            if (comment.getUserId() != null) {
                userIds.add(comment.getUserId());
            }
            if (comment.getReplyUserId() != null) {
                userIds.add(comment.getReplyUserId());
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds));
        Map<Long, User> result = new HashMap<>();
        for (User user : users) {
            result.put(user.getId(), user);
        }
        return result;
    }

    private void fillAuthor(Map<String, Object> node, User user) {
        if (user == null) {
            node.put("authorUsername", "匿名用户");
            node.put("authorNickname", "匿名用户");
            node.put("authorAvatar", null);
            return;
        }
        node.put("authorUsername", user.getUsername());
        node.put("authorNickname", user.getNickname());
        node.put("authorAvatar", user.getAvatar());
    }

    private void fillReplyUser(Map<String, Object> node, User user) {
        if (user == null) {
            node.put("replyUsername", null);
            node.put("replyNickname", null);
            return;
        }
        node.put("replyUsername", user.getUsername());
        node.put("replyNickname", user.getNickname());
    }

    @Override
    @Transactional
    public Map<String, Object> addComment(Map<String, Object> body, Long userId) {
        Long postId = Long.valueOf(body.get("postId").toString());
        String content = (String) body.get("content");
        content = content == null ? "" : content.trim();
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        Long replyUserId = body.get("replyUserId") != null ? Long.valueOf(body.get("replyUserId").toString()) : null;

        if (content.isEmpty()) {
            throw new RuntimeException("评论内容不能为空");
        }
        if (content.length() > 1000) {
            throw new RuntimeException("评论内容不能超过1000个字符");
        }
        if (sensitiveWordService.containsSensitiveWord(content)) {
            throw new RuntimeException("评论包含敏感词，请修改后发布");
        }

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("游记不存在");
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyUserId(replyUserId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());

        commentMapper.insert(comment);

        refreshPostCommentCount(postId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", comment.getId());
        result.put("postId", comment.getPostId());
        result.put("userId", comment.getUserId());
        result.put("content", comment.getContent());
        result.put("createTime", comment.getCreateTime());
        return result;
    }

    @Override
    @Transactional
    public void deleteComment(Long id, Long userId) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人评论");
        }
        commentMapper.deleteById(id);
        refreshPostCommentCount(comment.getPostId());
    }

    private void refreshPostCommentCount(Long postId) {
        LambdaQueryWrapper<Comment> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Comment::getPostId, postId);
        long count = commentMapper.selectCount(countWrapper);
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount((int) count);
            postMapper.updateById(post);
        }
    }
}
