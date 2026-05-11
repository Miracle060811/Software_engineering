package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
