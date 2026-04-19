package com.travelmate.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return jwtUtil.generateToken(username);
        }
        return null;
    }

    public User getUserByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

}
