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

    @Select("SELECT p.id, p.user_id AS userId, p.title, p.content, p.images, p.destination, p.lat, p.lng, p.tags, " +
            "p.like_count AS likeCount, p.comment_count AS commentCount, p.collect_count AS collectCount, " +
            "p.view_count AS viewCount, p.status, p.visibility, p.create_time AS createTime, p.update_time AS updateTime, " +
            "u.username AS authorUsername, u.nickname AS authorNickname, u.avatar AS authorAvatar FROM tm_post p " +
            "LEFT JOIN tm_user u ON p.user_id = u.id " +
            "WHERE p.deleted = 0 AND p.status = 1 " +
            "ORDER BY p.create_time DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> selectPostsWithUser(@Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT p.id, p.user_id AS userId, p.title, p.content, p.images, p.destination, p.lat, p.lng, p.tags, " +
            "p.like_count AS likeCount, p.comment_count AS commentCount, p.collect_count AS collectCount, " +
            "p.view_count AS viewCount, p.status, p.visibility, p.create_time AS createTime, p.update_time AS updateTime, " +
            "u.username AS authorUsername, u.nickname AS authorNickname, u.avatar AS authorAvatar FROM tm_post p " +
            "LEFT JOIN tm_user u ON p.user_id = u.id " +
            "WHERE p.deleted = 0 AND p.status = 1 " +
            "AND p.user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY p.create_time DESC " +
            "LIMIT #{offset}, #{limit}</script>")
    List<Map<String, Object>> selectPostsByUserIds(@Param("userIds") List<Long> userIds, @Param("offset") int offset, @Param("limit") int limit);
}
