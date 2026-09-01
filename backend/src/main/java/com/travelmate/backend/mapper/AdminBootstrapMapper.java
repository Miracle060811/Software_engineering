package com.travelmate.backend.mapper;

import com.travelmate.backend.entity.AdminBootstrapToken;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AdminBootstrapMapper {
    @Insert("INSERT IGNORE INTO admin_bootstrap_token(secret_fingerprint, expires_at) VALUES(#{fingerprint}, #{expiresAt})")
    int createTokenIfAbsent(@Param("fingerprint") String fingerprint,
                            @Param("expiresAt") LocalDateTime expiresAt);

    @Select("""
            SELECT id, secret_fingerprint AS secretFingerprint, expires_at AS expiresAt,
                   used_at AS usedAt, attempt_count AS attemptCount,
                   last_attempt_ip AS lastAttemptIp, last_result AS lastResult,
                   create_time AS createTime
            FROM admin_bootstrap_token WHERE secret_fingerprint = #{fingerprint} FOR UPDATE
            """)
    AdminBootstrapToken selectForUpdate(@Param("fingerprint") String fingerprint);

    @Update("""
            UPDATE admin_bootstrap_token SET attempt_count = attempt_count + 1,
            last_attempt_ip = #{sourceIp}, last_result = #{result} WHERE id = #{tokenId}
            """)
    int recordAttempt(@Param("tokenId") Long tokenId,
                      @Param("sourceIp") String sourceIp,
                      @Param("result") String result);

    @Update("UPDATE admin_bootstrap_token SET used_at = #{usedAt} WHERE id = #{tokenId} AND used_at IS NULL")
    int markUsed(@Param("tokenId") Long tokenId, @Param("usedAt") LocalDateTime usedAt);

    @Insert("""
            INSERT INTO admin_bootstrap_audit(token_id, username, source_ip, result)
            VALUES(#{tokenId}, #{username}, #{sourceIp}, #{result})
            """)
    int insertAudit(@Param("tokenId") Long tokenId,
                    @Param("username") String username,
                    @Param("sourceIp") String sourceIp,
                    @Param("result") String result);
}
