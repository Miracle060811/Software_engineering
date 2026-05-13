package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT p.*, u.nickname, u.avatar FROM tm_post p " +
            "LEFT JOIN tm_user u ON p.user_id = u.id " +
            "WHERE p.deleted = 0 AND p.status = 1 " +
            "ORDER BY p.create_time DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> selectPostsWithUser(@Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT p.*, u.nickname, u.avatar FROM tm_post p " +
            "LEFT JOIN tm_user u ON p.user_id = u.id " +
            "WHERE p.deleted = 0 AND p.status = 1 " +
            "AND p.user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY p.create_time DESC " +
            "LIMIT #{offset}, #{limit}</script>")
    List<Map<String, Object>> selectPostsByUserIds(@Param("userIds") List<Long> userIds, @Param("offset") int offset, @Param("limit") int limit);
}
