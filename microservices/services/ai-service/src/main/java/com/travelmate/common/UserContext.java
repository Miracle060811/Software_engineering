package com.travelmate.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

    public Long getCurrentUserIdOrNull() {
        AuthenticatedUser user = getCurrentPrincipalOrNull();
        return user == null ? null : user.userId();
    }

    public Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new RuntimeException("用户未登录或Token无效");
        }
        return userId;
    }

    public AuthenticatedUser getCurrentPrincipalOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null;
    }
}
