package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import com.travelmate.entity.Notification;
import com.travelmate.mapper.AiChatMapper;
import com.travelmate.mapper.AiPlanMapper;
import com.travelmate.mapper.NotificationMapper;
import com.travelmate.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    @Value("${ai.deepseek.api-key:sk-demo-placeholder}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Autowired
    private AiPlanMapper aiPlanMapper;

    @Autowired
    private AiChatMapper aiChatMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    private static final String FALLBACK_PLAN = "{\"title\":\"默认推荐行程\",\"summary\":\"AI服务暂时不可用，以下为我们为您准备的精选行程模板\"," +
            "\"days\":[{\"day\":1,\"theme\":\"抵达与初探\",\"activities\":[{\"time\":\"14:00\"," +
            "\"name\":\"抵达酒店\",\"description\":\"办理入住，休息调整\",\"type\":\"酒店\",\"cost\":500}]}]," +
            "\"totalEstimatedCost\":2000}";

    private static final String PLAN_SYSTEM_PROMPT = "你是一个专业的旅游规划师。请根据用户的需求，生成一份详细的旅游行程规划。\n" +
            "必须以严格的JSON格式返回，结构如下：\n" +
            "{\n" +
            "  \"title\": \"行程标题\",\n" +
            "  \"summary\": \"行程摘要\",\n" +
            "  \"days\": [\n" +
            "    {\n" +
            "      \"day\": 1,\n" +
            "      \"date\": \"2026-06-01\",\n" +
            "      \"theme\": \"当天主题\",\n" +
            "      \"activities\": [\n" +
            "        {\"time\": \"09:00\", \"name\": \"活动名称\", \"description\": \"描述\", " +
            "\"type\": \"景点/餐厅/酒店/交通\", \"cost\": 100}\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"totalEstimatedCost\": 3000\n" +
            "}";

    // ======================== AI 行程生成 ========================

    @Override
    public AiPlan generatePlan(AiPlanCreateDTO dto, Long userId) {
        String userPrompt = String.format(
                "目的地：%s，出行天数：%d天，预算：%.0f元，出行人数：%d人，出行偏好：%s，出发日期：%s",
                dto.getDestination(), dto.getDays(),
                dto.getBudget() != null ? dto.getBudget().doubleValue() : 0.0,
                dto.getPeopleCount(), dto.getPreferences(), dto.getStartDate());

        String planContent = callDeepSeekForPlan(userPrompt);

        AiPlan plan = new AiPlan();
        plan.setUserId(userId);
        plan.setDestination(dto.getDestination());
        plan.setDays(dto.getDays());
        plan.setBudget(dto.getBudget());
        plan.setPeopleCount(dto.getPeopleCount());
        plan.setPreferences(dto.getPreferences());
        plan.setPlanContent(planContent);
        plan.setStatus(1);
        plan.setCreateTime(LocalDateTime.now());

        // 解析标题（尝试从 JSON 中提取）
        String title = extractTitle(planContent, dto.getDestination(), dto.getDays());
        plan.setTitle(title);

        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            try {
                plan.setStartDate(LocalDate.parse(dto.getStartDate()));
            } catch (Exception ignored) {
                plan.setStartDate(LocalDate.now());
            }
        } else {
            plan.setStartDate(LocalDate.now());
        }

        aiPlanMapper.insert(plan);
        return plan;
    }

    private String callDeepSeekForPlan(String userPrompt) {
        try {
            String body = buildRequestBody(
                    PLAN_SYSTEM_PROMPT,
                    userPrompt,
                    true);
            String response = doHttpPost(baseUrl + "/v1/chat/completions", body);
            return extractContent(response);
        } catch (Exception e) {
            return FALLBACK_PLAN;
        }
    }

    @Override
    public List<AiPlan> listMyPlans(Long userId) {
        LambdaQueryWrapper<AiPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPlan::getUserId, userId)
                .orderByDesc(AiPlan::getCreateTime);
        return aiPlanMapper.selectList(wrapper);
    }

    @Override
    public AiPlan getPlanById(Long id, Long userId) {
        AiPlan plan = aiPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException("行程不存在或无权访问");
        }
        return plan;
    }

    // ======================== AI 多轮对话 ========================

    @Override
    public AiChat chat(AiChatDTO dto, Long userId) {
        String sessionId = dto.getSessionId();

        // 加载历史消息
        LambdaQueryWrapper<AiChat> historyQuery = new LambdaQueryWrapper<>();
        historyQuery.eq(AiChat::getUserId, userId)
                .eq(AiChat::getSessionId, sessionId)
                .orderByAsc(AiChat::getCreateTime)
                .last("LIMIT 20");
        List<AiChat> history = aiChatMapper.selectList(historyQuery);

        // 保存用户消息
        AiChat userMsg = new AiChat();
        userMsg.setUserId(userId);
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(dto.getMessage());
        userMsg.setCreateTime(LocalDateTime.now());
        aiChatMapper.insert(userMsg);

        // 构造多轮消息列表，调用 DeepSeek
        String aiReply = callDeepSeekForChat(history, dto.getMessage());

        // 保存 AI 回复
        AiChat assistantMsg = new AiChat();
        assistantMsg.setUserId(userId);
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiReply);
        assistantMsg.setCreateTime(LocalDateTime.now());
        aiChatMapper.insert(assistantMsg);

        return assistantMsg;
    }

    private String callDeepSeekForChat(List<AiChat> history, String userMessage) {
        try {
            StringBuilder messagesJson = new StringBuilder();
            messagesJson.append(
                    "{\"role\":\"system\",\"content\":\"你是TravelMate旅行助手，专注于旅游规划、景点推荐、行程建议等旅行相关问题。请用友好、专业的语气回答用户问题。\"}");

            for (AiChat msg : history) {
                messagesJson.append(",{\"role\":\"").append(escapeJson(msg.getRole()))
                        .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
            }
            messagesJson.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");

            String body = "{\"model\":\"deepseek-chat\",\"messages\":[" + messagesJson + "]}";
            String response = doHttpPost(baseUrl + "/v1/chat/completions", body);
            return extractContent(response);
        } catch (Exception e) {
            return "抱歉，AI助手暂时不可用，请稍后再试。";
        }
    }

    // ======================== 通知 ========================

    @Override
    public List<Notification> listNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectList(wrapper);
    }

    @Override
    public void markRead(Long id, Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }

    @Override
    public long unreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    // ======================== 工具方法 ========================

    private String buildRequestBody(String systemPrompt, String userMessage, boolean jsonMode) {
        String messagesJson = "[{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}]";
        if (jsonMode) {
            return "{\"model\":\"deepseek-chat\",\"messages\":" + messagesJson +
                    ",\"response_format\":{\"type\":\"json_object\"}}";
        }
        return "{\"model\":\"deepseek-chat\",\"messages\":" + messagesJson + "}";
    }

    private String doHttpPost(String url, String body) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API 响应异常: " + response.statusCode());
        }
        return response.body();
    }

    /**
     * 从 DeepSeek 响应 JSON 中提取 choices[0].message.content
     * 简单字符串解析，避免引入额外 JSON 库依赖
     */
    private String extractContent(String responseJson) {
        String marker = "\"content\":\"";
        // 跳过第一次出现（可能是 system message），找 choices 里的 content
        int choicesIdx = responseJson.indexOf("\"choices\"");
        if (choicesIdx < 0)
            return FALLBACK_PLAN;
        int idx = responseJson.indexOf(marker, choicesIdx);
        if (idx < 0)
            return FALLBACK_PLAN;
        int start = idx + marker.length();
        // 找结束引号（需处理转义）
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < responseJson.length(); i++) {
            char c = responseJson.charAt(i);
            if (escape) {
                if (c == 'n')
                    sb.append('\n');
                else if (c == 't')
                    sb.append('\t');
                else if (c == '\\')
                    sb.append('\\');
                else if (c == '"')
                    sb.append('"');
                else
                    sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String extractTitle(String planContent, String destination, int days) {
        try {
            String marker = "\"title\":\"";
            int idx = planContent.indexOf(marker);
            if (idx >= 0) {
                int start = idx + marker.length();
                int end = planContent.indexOf("\"", start);
                if (end > start) {
                    return planContent.substring(start, end);
                }
            }
        } catch (Exception ignored) {
        }
        return destination + " " + days + "日游";
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
