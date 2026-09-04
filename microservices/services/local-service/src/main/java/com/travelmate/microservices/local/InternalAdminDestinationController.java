package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Destination;
import com.travelmate.mapper.DestinationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/local/admin/destinations")
public class InternalAdminDestinationController {
    private final DestinationMapper destinations;
    private final String token;

    public InternalAdminDestinationController(DestinationMapper destinations,
                                              @Value("${app.internal-service-token}") String token) {
        this.destinations = destinations;
        this.token = token;
    }

    @GetMapping
    public List<Destination> list(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return destinations.selectList(new LambdaQueryWrapper<Destination>()
                .orderByAsc(Destination::getSortOrder).orderByDesc(Destination::getId));
    }

    @PostMapping("/sync-home")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncHome(@RequestBody List<Destination> incoming,
                                        @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        if (incoming == null || incoming.isEmpty()) bad("首页城市资源不能为空");
        if (incoming.size() > 100) bad("单次最多同步 100 个城市");

        int inserted = 0;
        int updated = 0;
        Set<String> slugs = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < incoming.size(); i++) {
            Destination destination = incoming.get(i);
            if (destination == null) bad("第 " + (i + 1) + " 个城市资源不能为空");
            destination.setId(null);
            destination.setSlug(destination.getSlug() == null ? null
                    : destination.getSlug().trim().toLowerCase(Locale.ROOT));
            if (!slugs.add(destination.getSlug())) bad("首页城市标识重复：" + destination.getSlug());
            if (destination.getCountry() == null || destination.getCountry().isBlank()) destination.setCountry("中国");
            if (destination.getSortOrder() == null) destination.setSortOrder((i + 1) * 10);
            if (destination.getStatus() == null) destination.setStatus(1);
            validate(destination);

            Destination existing = destinations.selectOne(new LambdaQueryWrapper<Destination>()
                    .eq(Destination::getSlug, destination.getSlug()).last("LIMIT 1"));
            destination.setUpdateTime(now);
            if (existing == null) {
                destination.setCreateTime(now);
                destinations.insert(destination);
                inserted++;
            } else {
                destination.setId(existing.getId());
                destination.setCreateTime(existing.getCreateTime());
                destinations.updateById(destination);
                updated++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", incoming.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        return result;
    }

    @DeleteMapping("/{id}")
    public void disable(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        Destination destination = destinations.selectById(id);
        if (destination == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "城市不存在");
        destination.setStatus(0);
        destination.setUpdateTime(LocalDateTime.now());
        if (destinations.updateById(destination) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "城市不存在");
    }

    private void validate(Destination destination) {
        text(destination.getSlug(), "城市标识"); text(destination.getName(), "城市名称");
        text(destination.getCountry(), "国家/地区"); text(destination.getTag(), "城市标签");
        text(destination.getImg(), "封面图"); text(destination.getDesc(), "短描述"); text(destination.getIntro(), "城市介绍");
    }

    private void text(String value, String field) { if (value == null || value.isBlank()) bad(field + "不能为空"); }
    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void verify(String supplied) { if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务令牌无效"); }
}
