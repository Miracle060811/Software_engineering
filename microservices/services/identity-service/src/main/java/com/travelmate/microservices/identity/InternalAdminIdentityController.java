package com.travelmate.microservices.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/identity/admin")
public class InternalAdminIdentityController {
    private final UserMapper mapper;
    private final String token;

    public InternalAdminIdentityController(UserMapper mapper, @Value("${app.internal-service-token}") String token) {
        this.mapper = mapper;
        this.token = token;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return mapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime)).stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId()); item.put("username", user.getUsername());
            item.put("nickname", user.getNickname()); item.put("role", user.getRole());
            item.put("status", user.getStatus()); item.put("createTime", user.getCreateTime());
            return item;
        }).toList();
    }

    @GetMapping("/count")
    public long count(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return mapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
    }

    @PostMapping("/users/{id}/disable")
    public void disable(@PathVariable Long id, @RequestParam Long adminId,
                        @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        if (id.equals(adminId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能禁用当前管理员账号");
        requireUser(id);
        mapper.update(null, new UpdateWrapper<User>().eq("id", id).set("status", 0)
                .setSql("token_version = token_version + 1"));
    }

    @PostMapping("/users/{id}/enable")
    public void enable(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        requireUser(id);
        mapper.update(null, new UpdateWrapper<User>().eq("id", id).set("status", 1));
    }

    private void verify(String supplied) {
        if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
    }

    private void requireUser(Long id) {
        if (mapper.selectById(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
    }
}
