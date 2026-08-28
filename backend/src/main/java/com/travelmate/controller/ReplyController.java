package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.Reply;
import com.travelmate.mapper.ReplyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reply")
public class ReplyController {

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private UserContext userContext;

    @GetMapping("/list")
    public Result<List<Reply>> listByReview(@RequestParam Long reviewId) {
        return Result.success(replyMapper.selectList(
                new LambdaQueryWrapper<Reply>()
                        .eq(Reply::getReviewId, reviewId)
                        .eq(Reply::getDeleted, 0)
                        .orderByAsc(Reply::getCreateTime)));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Reply reply) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        reply.setUserId(userId);
        reply.setCreateTime(LocalDateTime.now());
        reply.setDeleted(0);
        replyMapper.insert(reply);
        return Result.success("回复成功");
    }

    private Long getCurrentUserId() {
        return userContext.getCurrentUserIdOrNull();
    }
}
