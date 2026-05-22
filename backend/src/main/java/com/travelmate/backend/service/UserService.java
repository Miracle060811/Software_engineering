package com.travelmate.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public boolean register(String username, String password, Integer role) {
        Long count = userMapper.selectCount(new QueryWrapper<User>().eq("username", username));
        if (count > 0) {
            return false;
        }
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRole(role);
        userMapper.insert(user);
        return true;
    }

    public String login(String username, String password) {
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", username)
                .eq("status", 1)
                .eq("deleted", 0));
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return jwtUtil.generateToken(username);
        }
        return null;
    }

    public User getUserByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", username)
                .eq("status", 1)
                .eq("deleted", 0));
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return true;
    }

    public boolean resetPassword(String username, String newPassword) {
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", username)
                .eq("status", 1)
                .eq("deleted", 0));
        if (user == null) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return true;
    }

    public boolean deleteAccount(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) return false;
        if (!passwordEncoder.matches(password, user.getPassword())) return false;

        user.setUsername("deleted_" + userId);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setNickname("已注销用户");
        user.setAvatar(null);
        user.setEmail(null);
        user.setPhone(null);
        user.setBio(null);
        user.setRole(0);
        user.setStatus(0);
        user.setDeleted(1);
        userMapper.updateById(user);
        return true;
    }

}
