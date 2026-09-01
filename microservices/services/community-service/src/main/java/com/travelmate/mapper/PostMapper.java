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

    @Select("<script>SELECT p.id, p.user_id AS userId, p.title, p.content, p.images, p.destination, p.lat, p.lng, p.tags, " +
            "p.like_count AS likeCount, p.comment_count AS commentCount, p.collect_count AS collectCount, " +
            "p.view_count AS viewCount, p.status, p.visibility, p.create_time AS createTime, p.update_time AS updateTime FROM tm_post p " +
            "WHERE p.deleted = 0 AND p.status = 1 AND p.visibility = 0 " +
            "<if test='keyword != null'>AND (p.title LIKE #{keyword} OR p.content LIKE #{keyword} OR p.tags LIKE #{keyword} OR p.destination LIKE #{keyword}) </if>" +
            "ORDER BY (COALESCE(p.like_count,0) * 3 + COALESCE(p.comment_count,0) * 5 + COALESCE(p.collect_count,0) * 4 + COALESCE(p.view_count,0)) / POW(TIMESTAMPDIFF(HOUR, p.create_time, NOW()) + 2, 0.8) DESC, p.create_time DESC " +
            "LIMIT #{offset}, #{limit}</script>")
    List<Map<String, Object>> selectPostsWithUser(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("<script>SELECT p.id, p.user_id AS userId, p.title, p.content, p.images, p.destination, p.lat, p.lng, p.tags, " +
            "p.like_count AS likeCount, p.comment_count AS commentCount, p.collect_count AS collectCount, " +
            "p.view_count AS viewCount, p.status, p.visibility, p.create_time AS createTime, p.update_time AS updateTime FROM tm_post p " +
            "WHERE p.deleted = 0 AND p.status = 1 AND p.visibility IN (0, 1) " +
            "AND p.user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "<if test='keyword != null'>AND (p.title LIKE #{keyword} OR p.content LIKE #{keyword} OR p.tags LIKE #{keyword} OR p.destination LIKE #{keyword}) </if>" +
            "ORDER BY p.create_time DESC " +
            "LIMIT #{offset}, #{limit}</script>")
    List<Map<String, Object>> selectPostsByUserIds(@Param("userIds") List<Long> userIds, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);
}
