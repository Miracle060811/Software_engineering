package com.travelmate.common;

import java.security.Principal;

/**
 * JWT 校验后写入 Spring SecurityContext 的最小身份快照。
 *
 * <p>业务服务只依赖稳定用户标识和角色，不直接读取身份服务的数据表。</p>
 */
public record AuthenticatedUser(Long userId, String username, Integer role, Integer tokenVersion) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public boolean isAdmin() {
        return Integer.valueOf(1).equals(role);
    }
}
