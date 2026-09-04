package com.travelmate.microservices.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import com.travelmate.mapper.AiChatMapper;
import com.travelmate.mapper.AiPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AiPlanChatService {
    private static final Logger log = LoggerFactory.getLogger(AiPlanChatService.class);
    private static final String CHAT_SYSTEM_PROMPT = """
            你是 TravelMate 的中文旅行助手。请自然、简洁地回答路线、交通、住宿、预算和旅行风险问题。
            不要编造实时天气、余票、库存、价格或开放状态；没有可靠实时数据时要明确提醒用户出发前核验。
            用户信息不足时，最多追问一个关键问题，同时先给可执行的初步建议。
            普通回答控制在300字以内，不要输出思维链、工具协议或复杂 Markdown。
            """;
    private final AiPlanMapper planMapper;
    private final AiChatMapper chatMapper;
    private final ObjectMapper objectMapper;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.chat-completions-path:/chat/completions}")
    private String chatCompletionsPath;

    @Value("${ai.deepseek.chat-model:deepseek-v4-flash}")
    private String chatModel;

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
        List<AiChat> history = chatMapper.selectList(new LambdaQueryWrapper<AiChat>()
                .eq(AiChat::getUserId, userId)
                .eq(AiChat::getSessionId, sessionId)
                .orderByAsc(AiChat::getCreateTime)
                .last("LIMIT 20"));
        saveChat(userId, sessionId, "user", message);
        String reply = callDeepSeek(history, message);
        return saveChat(userId, sessionId, "assistant", reply);
    }

    private String callDeepSeek(List<AiChat> history, String userMessage) {
        if (apiKey == null || apiKey.isBlank() || "sk-demo-placeholder".equals(apiKey)) {
            return buildLocalReply(userMessage);
        }
        try {
            ArrayNode messages = objectMapper.createArrayNode();
            messages.addObject().put("role", "system").put("content", CHAT_SYSTEM_PROMPT);
            if (history != null) {
                for (AiChat item : history) {
                    if (item == null || item.getContent() == null
                            || !("user".equals(item.getRole()) || "assistant".equals(item.getRole()))) continue;
                    messages.addObject().put("role", item.getRole()).put("content", item.getContent());
                }
            }
            messages.addObject().put("role", "user").put("content", userMessage);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", resolveModel(chatModel));
            body.set("messages", messages);
            body.put("max_tokens", 1200);

            HttpRequest request = HttpRequest.newBuilder(URI.create(resolveChatUrl()))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DeepSeek chat returned HTTP {}", response.statusCode());
                return buildLocalReply(userMessage);
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            return content.isEmpty() ? buildLocalReply(userMessage) : content;
        } catch (Exception e) {
            log.warn("DeepSeek chat failed: {}", e.getMessage());
            return buildLocalReply(userMessage);
        }
    }

    private String resolveChatUrl() {
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (normalizedBase.endsWith("/chat/completions")) return normalizedBase;
        String path = chatCompletionsPath == null || chatCompletionsPath.isBlank()
                ? "/chat/completions" : chatCompletionsPath.trim();
        return normalizedBase + (path.startsWith("/") ? path : "/" + path);
    }

    private String resolveModel(String configured) {
        if (configured == null || configured.isBlank()) return "deepseek-chat";
        String value = configured.trim();
        // DeepSeek's official API exposes these stable model aliases.
        if (baseUrl != null && baseUrl.contains("api.deepseek.com") && value.startsWith("deepseek-v4")) {
            return "deepseek-chat";
        }
        return value;
    }

    private String buildLocalReply(String message) {
        if (message != null && message.matches("^(你好|您好|hi|hello|嗨|在吗)[。！!\\s]*$")) {
            return "在呢。告诉我目的地、天数和预算，我可以帮你一起梳理路线。";
        }
        if (message != null && (message.contains("酒店") || message.contains("住宿"))) {
            return "选酒店或住宿区域时，先看每天的活动半径，再比较预算、交通和夜间返回是否方便。告诉我城市和预算，我可以继续帮你缩小区域。";
        }
        return "我可以帮你分析路线、住宿、交通和预算。请补充目的地和出行天数，我先给你一版可执行建议。";
    }

    private AiChat saveChat(Long userId, String sessionId, String role, String content) {
        AiChat chat = new AiChat();
        chat.setUserId(userId); chat.setSessionId(sessionId); chat.setRole(role);
        chat.setContent(content); chat.setCreateTime(LocalDateTime.now());
        chatMapper.insert(chat);
        return chat;
    }

}
