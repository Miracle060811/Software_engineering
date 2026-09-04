package com.travelmate.microservices.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import com.travelmate.mapper.AiChatMapper;
import com.travelmate.mapper.AiPlanMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    @org.springframework.beans.factory.annotation.Autowired
    private AiItineraryService itineraryService;

    public AiPlan generate(AiPlanCreateDTO dto, Long userId) {
        return itineraryService.generatePlan(dto, userId);
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

}
