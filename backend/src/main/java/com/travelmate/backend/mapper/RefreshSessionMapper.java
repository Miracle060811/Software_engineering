package com.travelmate.backend.mapper;

import com.travelmate.backend.entity.RefreshSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RefreshSessionMapper {
    @Insert("""
            INSERT INTO auth_refresh_session
            (id, user_id, token_hash, token_version, expires_at, source_ip, user_agent)
            VALUES(#{id}, #{userId}, #{tokenHash}, #{tokenVersion}, #{expiresAt}, #{sourceIp}, #{userAgent})
            """)
    int insert(RefreshSession session);

    @Select("""
            SELECT id, user_id AS userId, token_hash AS tokenHash, token_version AS tokenVersion,
                   expires_at AS expiresAt, revoked_at AS revokedAt, source_ip AS sourceIp,
                   user_agent AS userAgent, create_time AS createTime, last_used_at AS lastUsedAt
            FROM auth_refresh_session WHERE token_hash = #{tokenHash} FOR UPDATE
            """)
    RefreshSession selectForUpdate(@Param("tokenHash") String tokenHash);

    @Update("""
            UPDATE auth_refresh_session SET revoked_at = #{now}, last_used_at = #{now}
            WHERE id = #{id} AND revoked_at IS NULL
            """)
    int revoke(@Param("id") String id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE auth_refresh_session SET revoked_at = #{now}
            WHERE user_id = #{userId} AND revoked_at IS NULL
            """)
    int revokeAll(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
