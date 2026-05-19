package com.travelmate.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

    @Autowired
    private UserMapper userMapper;

    public Long getCurrentUserIdOrNull() {
        User user = getCurrentUserOrNull();
        return user == null ? null : user.getId();
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public User getCurrentUserOrNull() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }

        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .and(wrapper -> wrapper.eq(User::getDeleted, 0)
                        .or()
                        .isNull(User::getDeleted)));
    }

    public User getCurrentUser() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            throw new RuntimeException("用户未登录或Token无效");
        }
        return user;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }

        return username;
    }
}
