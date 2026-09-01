package com.travelmate.microservices.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import com.travelmate.mapper.AiChatMapper;
import com.travelmate.mapper.AiPlanMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AiPlanChatService {
    private final AiPlanMapper planMapper;
    private final AiChatMapper chatMapper;
    private final ObjectMapper objectMapper;

    public AiPlanChatService(AiPlanMapper planMapper, AiChatMapper chatMapper, ObjectMapper objectMapper) {
        this.planMapper = planMapper;
        this.chatMapper = chatMapper;
        this.objectMapper = objectMapper;
    }

    public AiPlan generate(AiPlanCreateDTO dto, Long userId) {
        if (dto == null) throw new RuntimeException("行程参数不能为空");
        String origin = required(dto.getOrigin(), "出发地不能为空");
        String destination = required(dto.getDestination(), "目的地不能为空");
        if (dto.getDays() < 1 || dto.getDays() > 30) throw new RuntimeException("行程天数必须在1到30天之间");
        LocalDate startDate;
        try {
            startDate = dto.getStartDate() == null || dto.getStartDate().isBlank()
                    ? LocalDate.now().plusDays(7) : LocalDate.parse(dto.getStartDate());
        } catch (DateTimeParseException e) {
            throw new RuntimeException("开始日期格式必须为YYYY-MM-DD");
        }

        List<Map<String, Object>> days = new ArrayList<>();
        for (int day = 1; day <= dto.getDays(); day++) {
            days.add(Map.of("day", day, "title", destination + "第" + day + "天",
                    "activities", List.of("城市漫步", "当地美食体验")));
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("origin", origin); content.put("destination", destination);
        content.put("locationVerified", true); content.put("days", days);
        content.put("travelStyle", dto.getTravelStyle());

        AiPlan plan = new AiPlan();
        plan.setUserId(userId); plan.setTitle(destination + dto.getDays() + "日行程");
        plan.setDestination(destination); plan.setStartDate(startDate); plan.setDays(dto.getDays());
        plan.setBudget(dto.getBudget()); plan.setPeopleCount(Math.max(dto.getPeopleCount(), 1));
        plan.setPreferences(dto.getPreferences()); plan.setPlanContent(writeJson(content));
        plan.setStatus(1); plan.setCreateTime(LocalDateTime.now());
        planMapper.insert(plan);
        return plan;
    }

    public List<AiPlan> list(Long userId) {
        return planMapper.selectList(new LambdaQueryWrapper<AiPlan>()
                .eq(AiPlan::getUserId, userId).orderByDesc(AiPlan::getCreateTime));
    }

    public AiPlan get(Long id, Long userId) {
        AiPlan plan = planMapper.selectById(id);
        if (plan == null) throw new RuntimeException("行程不存在");
        if (!Objects.equals(plan.getUserId(), userId)) throw new RuntimeException("无权访问该行程");
        return plan;
    }

    public AiChat chat(AiChatDTO dto, Long userId) {
        if (dto == null || dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            throw new RuntimeException("消息不能为空");
        }
        String message = dto.getMessage().trim();
        if (message.length() > 2000) throw new RuntimeException("消息不能超过2000个字符");
        String sessionId = dto.getSessionId() == null || dto.getSessionId().isBlank()
                ? UUID.randomUUID().toString() : dto.getSessionId().trim();
        saveChat(userId, sessionId, "user", message);
        String reply = message.contains("酒店")
                ? "酒店建议优先比较位置、预算、交通便利度和近期住客评价。"
                : "请告诉我目的地、出行日期、预算和偏好，我会继续完善行程建议。";
        return saveChat(userId, sessionId, "assistant", reply);
    }

    private AiChat saveChat(Long userId, String sessionId, String role, String content) {
        AiChat chat = new AiChat();
        chat.setUserId(userId); chat.setSessionId(sessionId); chat.setRole(role);
        chat.setContent(content); chat.setCreateTime(LocalDateTime.now());
        chatMapper.insert(chat);
        return chat;
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new RuntimeException(message);
        return value.trim();
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new RuntimeException("行程序列化失败", e); }
    }
}
