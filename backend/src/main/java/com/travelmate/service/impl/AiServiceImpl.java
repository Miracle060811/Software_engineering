package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.dto.PostAuditResult;
import com.travelmate.entity.*;
import com.travelmate.mapper.AiChatMapper;
import com.travelmate.mapper.AiPlanMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import com.travelmate.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.deepseek.api-key:sk-demo-placeholder}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.plan-model:deepseek-v4-flash}")
    private String planModel;

    @Value("${ai.deepseek.chat-model:deepseek-v4-flash}")
    private String chatModel;

    @Value("${ai.deepseek.thinking-enabled:false}")
    private boolean thinkingEnabled;

    @Value("${ai.deepseek.reasoning-effort:high}")
    private String reasoningEffort;

    @Autowired
    private AiPlanMapper aiPlanMapper;

    @Autowired
    private AiChatMapper aiChatMapper;

    @Autowired
    private NotificationCenterService notificationCenterService;

    @Autowired
    private SysSensitiveWordMapper sensitiveWordMapper;

    @Autowired(required = false)
    private FlightService flightService;

    @Autowired(required = false)
    private HotelService hotelService;

    private boolean apiKeyWarningLogged = false;

    private static final String PLAN_SYSTEM_PROMPT = "你是一个专业的旅游规划师。请根据用户的需求，生成一份详细的旅游行程规划。\n" +
            "必须以严格的JSON格式返回，不要包含markdown代码块标记，结构如下：\n" +
            "{\n" +
            "  \"title\": \"行程标题（需包含目的地名称）\",\n" +
            "  \"summary\": \"行程摘要（100字以内，概括行程亮点）\",\n" +
            "  \"days\": [\n" +
            "    {\n" +
            "      \"day\": 1,\n" +
            "      \"date\": \"2026-06-01\",\n" +
            "      \"theme\": \"当天主题\",\n" +
            "      \"activities\": [\n" +
            "        {\"time\": \"09:00\", \"name\": \"活动名称\", \"description\": \"详细描述\", " +
            "\"type\": \"景点/餐厅/酒店/交通/购物/娱乐\", \"cost\": 100}\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"totalEstimatedCost\": 3000\n" +
            "}\n" +
            "要求：\n" +
            "1. 每天至少包含4-5个活动（上午、中午、下午、晚上各至少1个），涵盖景点、餐饮、交通等\n" +
            "2. 活动描述要具体，包含地点名称和实用信息\n" +
            "3. 费用估算要合理，参考中国旅游实际消费水平\n" +
            "4. 行程要符合逻辑，考虑景点间距离和交通时间\n" +
            "5. totalEstimatedCost 为所有活动费用总和（不含酒店）";

    private static final String POST_AUDIT_SYSTEM_PROMPT = "你是 TravelMate 社区游记内容审核 AI。请只返回严格 JSON，不要输出 markdown。" +
            "审核目标：判断用户发布的旅行笔记是否可直接发布。" +
            "审核优先级：1) 命中 level=3 严重敏感词时必须 reject；" +
            "2) 命中 level=2 中度敏感词时从严审核，除非上下文明确无害；" +
            "3) level=1 轻度敏感词作为风险提示；" +
            "4) 再判断违法违规、辱骂仇恨、色情低俗、广告引流、诈骗、隐私泄露、明显非旅行内容。" +
            "返回格式：{\"decision\":\"approve|reject\",\"reason\":\"20字以内中文原因\"}。";

    // ======================== AI 行程生成 ========================

    @Override
    public AiPlan generatePlan(AiPlanCreateDTO dto, Long userId) {
        checkApiKey();

        String userPrompt = String.format(
                "目的地：%s，出行天数：%d天，预算：%.0f元，出行人数：%d人，出行偏好：%s，出发日期：%s",
                dto.getDestination(), dto.getDays(),
                dto.getBudget() != null ? dto.getBudget().doubleValue() : 0.0,
                dto.getPeopleCount(), dto.getPreferences(), dto.getStartDate());

        String planContent = callDeepSeekForPlan(userPrompt, dto);

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

        String title = extractFieldFromJson(planContent, "title");
        if (title == null || title.isEmpty()) {
            title = dto.getDestination() + " " + dto.getDays() + "日游";
        }
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
        notificationCenterService.createNotification(
                userId,
                "ai_plan",
                "AI 行程已生成",
                String.format("您的 %s %d 天行程已生成，可在行程列表中查看详情。", dto.getDestination(), dto.getDays()),
                "/ai-plan");
        return plan;
    }

    private void checkApiKey() {
        if ("sk-demo-placeholder".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            String fromFile = loadKeyFromEnvFile();
            if (fromFile != null && !fromFile.isBlank()) {
                this.apiKey = fromFile;
                log.info("已从 .env 文件自动加载 DeepSeek API Key ({}...{})",
                        fromFile.substring(0, 7), fromFile.substring(fromFile.length() - 4));
                return;
            }
            if (!apiKeyWarningLogged) {
                log.warn("DeepSeek API Key 未配置！AI 功能将使用降级模板。请在 .env 文件中设置 DEEPSEEK_API_KEY");
                apiKeyWarningLogged = true;
            }
        }
    }

    /**
     * 尝试从 .env 文件读取 DEEPSEEK_API_KEY
     * 依次尝试多个可能的路径：backend 目录的上级、当前目录、user.home
     */
    private String loadKeyFromEnvFile() {
        String[] searchPaths = { "../.env", ".env", "../../.env" };
        for (String sp : searchPaths) {
            try {
                Path path = Paths.get(sp);
                if (Files.exists(path)) {
                    String content = Files.readString(path);
                    for (String line : content.split("\n")) {
                        line = line.trim();
                        if (line.startsWith("DEEPSEEK_API_KEY") && line.contains("=")) {
                            String value = line.substring(line.indexOf('=') + 1).trim();
                            value = value.replaceAll("^[\"']|[\"']$", "");
                            if (!value.isBlank() && !value.startsWith("your_")
                                    && !value.equals("sk-demo-placeholder")) {
                                return value;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String callDeepSeekForPlan(String userPrompt, AiPlanCreateDTO dto) {
        try {
            String body = buildPlanRequestBody(userPrompt);
            log.info("正在调用 DeepSeek API 生成行程规划...");
            String response = doHttpPost(baseUrl + "/v1/chat/completions", body);
            String content = extractContent(response);
            if (content != null) {
                log.info("AI 行程生成成功");
                return content;
            }
            log.warn("AI 返回内容解析失败，使用降级模板");
        } catch (Exception e) {
            log.warn("AI 行程生成失败: {}，使用降级模板", e.getMessage());
        }
        return generateFallbackPlan(dto);
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

    // ======================== AI 多轮对话（含 Function Calling） ========================

    private static final String TOOLS_JSON = "[{\"type\":\"function\",\"function\":{\"name\":\"get_weather\"," +
            "\"description\":\"查询指定城市在指定日期的天气情况\",\"parameters\":{\"type\":\"object\",\"properties\":{" +
            "\"city\":{\"type\":\"string\",\"description\":\"城市名称，如北京\"}," +
            "\"date\":{\"type\":\"string\",\"description\":\"日期，格式yyyy-MM-dd\"}},\"required\":[\"city\",\"date\"]}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"search_flights\"," +
            "\"description\":\"搜索指定日期和航线的航班信息\",\"parameters\":{\"type\":\"object\",\"properties\":{" +
            "\"depCity\":{\"type\":\"string\",\"description\":\"出发城市，如北京\"}," +
            "\"arrCity\":{\"type\":\"string\",\"description\":\"到达城市，如上海\"}," +
            "\"date\":{\"type\":\"string\",\"description\":\"出发日期，格式yyyy-MM-dd\"}},\"required\":[\"depCity\",\"arrCity\"]}}},"
            +
            "{\"type\":\"function\",\"function\":{\"name\":\"search_hotels\"," +
            "\"description\":\"搜索指定城市的酒店列表\",\"parameters\":{\"type\":\"object\",\"properties\":{" +
            "\"city\":{\"type\":\"string\",\"description\":\"城市名称，如北京\"}},\"required\":[\"city\"]}}}]";

    @Override
    public AiChat chat(AiChatDTO dto, Long userId) {
        checkApiKey();

        String sessionId = dto.getSessionId();

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

        // 调用 DeepSeek（含 Function Calling）
        String aiReply = callDeepSeekWithTools(history, dto.getMessage());

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

    private String callDeepSeekWithTools(List<AiChat> history, String userMessage) {
        try {
            StringBuilder messagesJson = new StringBuilder();
            messagesJson.append(
                    "{\"role\":\"system\",\"content\":\"你是TravelMate旅行助手，专注于旅游规划、景点推荐、行程建议等旅行相关问题。" +
                            "当用户询问天气、航班、酒店等实时信息时，请调用对应的工具函数获取数据。请用友好、专业的语气回答用户问题。\"}");

            for (AiChat msg : history) {
                messagesJson.append(",{\"role\":\"").append(escapeJson(msg.getRole()))
                        .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
            }
            messagesJson.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");

            // 首次调用（含工具定义）
            String body = "{\"model\":\"" + escapeJson(resolveModel(chatModel)) + "\",\"messages\":[" + messagesJson +
                    "]" + buildThinkingConfigJson() + ",\"tools\":" + TOOLS_JSON + "}";
            log.info("正在调用 DeepSeek API (带工具)...");
            String response = doHttpPost(baseUrl + "/v1/chat/completions", body);

            // 检查是否有 tool_calls
            JsonNode toolCall = extractToolCall(response);
            if (toolCall != null) {
                log.info("检测到工具调用，执行中...");
                String toolResult = executeToolCall(toolCall);
                // 将工具结果追加到消息中，再次请求 AI 生成最终回复
                messagesJson.append(",{\"role\":\"assistant\",\"content\":\"\",\"tool_calls\":[")
                        .append(toolCall.toString()).append("]}");
                messagesJson.append(",{\"role\":\"tool\",\"content\":\"")
                        .append(escapeJson(toolResult)).append("\"}");
                String body2 = "{\"model\":\"" + escapeJson(resolveModel(chatModel)) + "\",\"messages\":["
                        + messagesJson + "]" + buildThinkingConfigJson() + "}";
                String response2 = doHttpPost(baseUrl + "/v1/chat/completions", body2);
                String content = extractContent(response2);
                if (content != null)
                    return content;
            }

            String content = extractContent(response);
            if (content != null)
                return content;
            log.warn("AI 对话响应解析失败");
        } catch (Exception e) {
            log.warn("AI 对话失败: {}", e.getMessage());
        }
        return "抱歉，AI助手暂时不可用，请稍后再试。您也可以尝试在社区中搜索相关旅行攻略。";
    }

    // ======================== 游记自动审核 ========================

    @Override
    public PostAuditResult auditPost(String title, String content, String tags, String destination) {
        List<SysSensitiveWord> matchedWords = findMatchedSensitiveWords(title, content, tags, destination);

        checkApiKey();
        if (!isApiKeyMissing()) {
            try {
                String response = doHttpPost(baseUrl + "/v1/chat/completions",
                        buildPostAuditRequestBody(title, content, tags, destination, matchedWords));
                String auditJson = extractContent(response);
                PostAuditResult result = parsePostAuditResult(auditJson);
                if (result != null) {
                    return result;
                }
                log.warn("AI 游记审核响应解析失败，使用本地等级规则降级");
            } catch (Exception e) {
                log.warn("AI 游记审核失败: {}，使用本地等级规则降级", e.getMessage());
            }
        }

        return fallbackPostAudit(matchedWords);
    }

    private boolean isApiKeyMissing() {
        return apiKey == null || apiKey.isBlank() || "sk-demo-placeholder".equals(apiKey);
    }

    private List<SysSensitiveWord> findMatchedSensitiveWords(String... values) {
        StringBuilder text = new StringBuilder();
        for (String value : values) {
            if (value != null) {
                text.append(value).append('\n');
            }
        }
        String allText = text.toString();
        if (allText.isBlank()) {
            return List.of();
        }
        return sensitiveWordMapper.selectList(null).stream()
                .filter(word -> word.getWord() != null && !word.getWord().isBlank())
                .filter(word -> allText.contains(word.getWord()))
                .sorted(Comparator.comparing((SysSensitiveWord word) -> word.getLevel() == null ? 1 : word.getLevel())
                        .reversed())
                .toList();
    }

    private String buildPostAuditRequestBody(String title, String content, String tags, String destination,
            List<SysSensitiveWord> matchedWords) {
        String userPrompt = "请审核以下旅行笔记：\n" +
                "标题：" + safeAuditText(title, 200) + "\n" +
                "目的地：" + safeAuditText(destination, 100) + "\n" +
                "标签：" + safeAuditText(tags, 300) + "\n" +
                "正文：" + safeAuditText(content, 4000) + "\n" +
                "命中的敏感词及优先级：" + buildMatchedWordsJson(matchedWords);
        String messagesJson = "[{\"role\":\"system\",\"content\":\"" + escapeJson(POST_AUDIT_SYSTEM_PROMPT) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson(userPrompt) + "\"}]";
        return "{\"model\":\"" + escapeJson(resolveModel(chatModel)) + "\",\"messages\":" + messagesJson +
                buildThinkingConfigJson() +
                ",\"response_format\":{\"type\":\"json_object\"}}";
    }

    private String safeAuditText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String buildMatchedWordsJson(List<SysSensitiveWord> matchedWords) {
        if (matchedWords.isEmpty()) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < matchedWords.size(); i++) {
            SysSensitiveWord word = matchedWords.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"word\":\"").append(escapeJson(word.getWord())).append("\",\"level\":")
                    .append(word.getLevel() == null ? 1 : word.getLevel()).append('}');
        }
        json.append(']');
        return json.toString();
    }

    private PostAuditResult parsePostAuditResult(String auditJson) {
        if (auditJson == null || auditJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(auditJson);
            String decision = root.path("decision").asText("");
            String reason = root.path("reason").asText("AI自动审核");
            if ("approve".equalsIgnoreCase(decision)) {
                return new PostAuditResult(true, null);
            }
            if ("reject".equalsIgnoreCase(decision)) {
                return new PostAuditResult(false, safeAuditText(reason, 300));
            }
        } catch (Exception e) {
            log.warn("解析 AI 游记审核 JSON 失败: {}", e.getMessage());
        }
        return null;
    }

    private PostAuditResult fallbackPostAudit(List<SysSensitiveWord> matchedWords) {
        if (matchedWords.isEmpty()) {
            return new PostAuditResult(true, null);
        }
        int maxLevel = matchedWords.stream()
                .map(SysSensitiveWord::getLevel)
                .filter(level -> level != null)
                .max(Integer::compareTo)
                .orElse(1);
        if (maxLevel >= 2) {
            return new PostAuditResult(false, "命中中高风险敏感词");
        }
        return new PostAuditResult(true, null);
    }

    /**
     * 执行工具调用并返回结果字符串
     */
    private String executeToolCall(JsonNode toolCall) {
        try {
            JsonNode function = toolCall.get("function");
            if (function == null)
                return "{\"error\":\"工具调用格式错误\"}";
            String name = function.get("name").asText();
            String argsStr = function.get("arguments").asText();
            JsonNode args = objectMapper.readTree(argsStr);

            switch (name) {
                case "get_weather": {
                    String city = args.has("city") ? args.get("city").asText() : "未知城市";
                    return String.format("{\"city\":\"%s\",\"weather\":\"晴转多云\",\"temperature\":\"22°C ~ 28°C\"," +
                            "\"humidity\":\"65%%\",\"wind\":\"微风\",\"tips\":\"适合出行游玩\"}", city);
                }
                case "search_flights": {
                    String depCity = args.has("depCity") ? args.get("depCity").asText() : null;
                    String arrCity = args.has("arrCity") ? args.get("arrCity").asText() : null;
                    String date = args.has("date") ? args.get("date").asText() : null;
                    if (flightService != null && depCity != null && arrCity != null) {
                        List<Flight> flights = flightService.searchFlights(depCity, arrCity, date);
                        StringBuilder sb = new StringBuilder("{\"flights\":[");
                        int count = 0;
                        for (Flight f : flights) {
                            if (count++ > 0)
                                sb.append(",");
                            sb.append(String.format(
                                    "{\"flightNo\":\"%s\",\"airline\":\"%s\",\"departure\":\"%s\",\"arrival\":\"%s\",\"economyPrice\":%.2f}",
                                    f.getFlightNo(), f.getAirline(), f.getDepartureTime(), f.getArrivalTime(),
                                    f.getEconomyPrice()));
                            if (count >= 5)
                                break;
                        }
                        sb.append("]}");
                        return sb.toString();
                    }
                    return "{\"flights\":[{\"flightNo\":\"CA1234\",\"airline\":\"中国国航\",\"economyPrice\":680}]}";
                }
                case "search_hotels": {
                    String city = args.has("city") ? args.get("city").asText() : null;
                    if (hotelService != null && city != null) {
                        List<Hotel> hotels = hotelService.searchHotels(city, null, null, null, null, null);
                        StringBuilder sb = new StringBuilder("{\"hotels\":[");
                        int count = 0;
                        for (Hotel h : hotels) {
                            if (count++ > 0)
                                sb.append(",");
                            sb.append(String.format(
                                    "{\"name\":\"%s\",\"star\":%d,\"score\":%.1f,\"address\":\"%s\",\"avgPrice\":%.2f}",
                                    h.getName(), h.getStarRating(), h.getScore(), h.getAddress(), h.getAvgPrice()));
                            if (count >= 5)
                                break;
                        }
                        sb.append("]}");
                        return sb.toString();
                    }
                    return "{\"hotels\":[{\"name\":\"示例酒店\",\"star\":4,\"score\":4.5,\"avgPrice\":350}]}";
                }
                default:
                    return "{\"error\":\"未知工具: " + name + "\"}";
            }
        } catch (Exception e) {
            log.warn("工具执行异常: {}", e.getMessage());
            return "{\"error\":\"工具执行异常: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 从 DeepSeek 响应中提取第一个 tool_call
     */
    private JsonNode extractToolCall(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode toolCalls = message.get("tool_calls");
                    if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                        return toolCalls.get(0);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析 tool_calls 失败: {}", e.getMessage());
        }
        return null;
    }

    // ======================== 通知 ========================

    @Override
    public List<Notification> listNotifications(Long userId) {
        return notificationCenterService.listNotifications(userId);
    }

    @Override
    public void markRead(Long id, Long userId) {
        notificationCenterService.markRead(id, userId);
    }

    @Override
    public void deleteNotification(Long id, Long userId) {
        notificationCenterService.deleteNotification(id, userId);
    }

    @Override
    public void deleteAllNotifications(Long userId) {
        notificationCenterService.deleteAllNotifications(userId);
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationCenterService.unreadCount(userId);
    }

    // ======================== JSON 解析工具方法（使用 Jackson） ========================

    /**
     * 从 DeepSeek API 响应 JSON 中提取 choices[0].message.content
     */
    private String extractContent(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null && !content.isNull()) {
                        String text = content.asText();
                        // 去掉可能的 markdown 代码块标记
                        if (text.startsWith("```json")) {
                            text = text.substring(7);
                        }
                        if (text.startsWith("```")) {
                            text = text.substring(3);
                        }
                        if (text.endsWith("```")) {
                            text = text.substring(0, text.length() - 3);
                        }
                        return text.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 DeepSeek 响应 JSON 失败: {}", e.getMessage());
        }
        return null;
    }

    private String extractFieldFromJson(String json, String fieldName) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode field = root.get(fieldName);
            return field != null ? field.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ======================== HTTP 请求 ========================

    private String buildPlanRequestBody(String userMessage) {
        String messagesJson = "[{\"role\":\"system\",\"content\":\"" + escapeJson(PLAN_SYSTEM_PROMPT) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}]";
        return "{\"model\":\"" + escapeJson(resolveModel(planModel)) + "\",\"messages\":" + messagesJson +
                buildThinkingConfigJson() +
                ",\"response_format\":{\"type\":\"json_object\"}}";
    }

    private String buildThinkingConfigJson() {
        StringBuilder json = new StringBuilder();
        json.append(",\"thinking\":{\"type\":\"")
                .append(thinkingEnabled ? "enabled" : "disabled")
                .append("\"}");

        if (thinkingEnabled) {
            json.append(",\"reasoning_effort\":\"")
                    .append(escapeJson(resolveReasoningEffort(reasoningEffort)))
                    .append("\"");
        }

        return json.toString();
    }

    private String resolveModel(String configuredModel) {
        return configuredModel == null || configuredModel.isBlank() ? "deepseek-v4-flash" : configuredModel;
    }

    private String resolveReasoningEffort(String configuredEffort) {
        if (configuredEffort == null || configuredEffort.isBlank()) {
            return "high";
        }

        return switch (configuredEffort.trim().toLowerCase()) {
            case "xhigh", "max" -> "max";
            case "low", "medium", "high" -> "high";
            default -> "high";
        };
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
                .timeout(Duration.ofSeconds(60))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            log.error("DeepSeek API 认证失败！请检查 .env 中的 DEEPSEEK_API_KEY 是否正确");
            throw new RuntimeException("API 认证失败 (HTTP " + status + ")，请检查 API Key 配置");
        }
        if (status != 200) {
            log.error("DeepSeek API 响应异常: HTTP {}", status);
            throw new RuntimeException("DeepSeek API 响应异常: " + status);
        }
        return response.body();
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

    // ======================== 降级行程模版 ========================

    /**
     * 当 AI 服务不可用时，根据用户输入生成丰富的默认行程模版
     */
    private String generateFallbackPlan(AiPlanCreateDTO dto) {
        String dest = dto.getDestination();
        int days = Math.min(dto.getDays(), 7);
        int people = Math.max(dto.getPeopleCount(), 1);
        double budget = dto.getBudget() != null ? dto.getBudget().doubleValue() : 5000;
        String prefs = dto.getPreferences() != null ? dto.getPreferences() : "";

        StringBuilder json = new StringBuilder();
        json.append("{\"title\":\"").append(escapeJson(dest)).append(" ").append(days).append("日深度游\",");
        json.append("\"summary\":\"")
                .append(escapeJson("AI服务暂时不可用，以下是我们为您精心准备的" + dest + days + "日行程模板，涵盖经典景点、特色美食与实用贴士。实际行程可根据您的偏好灵活调整。"))
                .append("\",");
        json.append("\"days\":[");

        double totalCost = 0;
        for (int d = 1; d <= days; d++) {
            if (d > 1)
                json.append(",");
            DayPlan dayPlan = buildDayPlan(dest, d, days, prefs, people, budget / days);
            json.append(dayPlan.json);
            totalCost += dayPlan.cost;
        }

        json.append("],");
        json.append("\"totalEstimatedCost\":").append((int) totalCost);
        json.append("}");

        return json.toString();
    }

    private static class DayPlan {
        String json;
        double cost;

        DayPlan(String json, double cost) {
            this.json = json;
            this.cost = cost;
        }
    }

    private DayPlan buildDayPlan(String dest, int day, int totalDays, String prefs, int people, double dayBudget) {
        String[] themes = getThemesForDestination(dest);
        String theme = themes[(day - 1) % themes.length];

        StringBuilder sb = new StringBuilder();
        sb.append("{\"day\":").append(day).append(",\"theme\":\"").append(escapeJson(theme))
                .append("\",\"activities\":[");

        double dayCost = 0;
        String[] activities = getActivitiesForDestination(dest, day, totalDays);
        String[] times = { "08:00", "10:00", "12:00", "14:30", "17:00", "19:00" };
        String[] types = { "交通", "景点", "餐饮", "景点", "购物", "餐饮" };
        int[] costs = { 50, 100, 60, 80, 100, 80 };

        for (int i = 0; i < activities.length; i++) {
            if (i > 0)
                sb.append(",");
            int cost = (int) (costs[i] * people * (dayBudget / 500.0));
            if (cost < 10)
                cost = 10;
            dayCost += cost;
            sb.append("{\"time\":\"").append(times[i]).append("\",");
            sb.append("\"name\":\"").append(escapeJson(activities[i])).append("\",");
            sb.append("\"description\":\"").append(escapeJson(getActivityDescription(activities[i], dest)))
                    .append("\",");
            sb.append("\"type\":\"").append(types[i]).append("\",");
            sb.append("\"cost\":").append(cost).append("}");
        }

        sb.append("]}");
        return new DayPlan(sb.toString(), dayCost);
    }

    private String[] getThemesForDestination(String dest) {
        if (dest.contains("北京"))
            return new String[] { "皇城根下初探", "长城雄关之旅", "胡同文化与美食", "皇家园林漫步", "艺术与时尚碰撞" };
        if (dest.contains("上海"))
            return new String[] { "外滩与陆家嘴", "老城厢与弄堂", "艺术与博物馆", "迪士尼奇妙日", "法租界浪漫游" };
        if (dest.contains("成都"))
            return new String[] { "巴蜀文化初体验", "熊猫与美食之旅", "古镇与茶文化", "自然风光探索" };
        if (dest.contains("西安"))
            return new String[] { "秦风汉韵", "盛唐风华", "城墙与回民街", "周边古迹探秘" };
        if (dest.contains("杭州"))
            return new String[] { "西湖十景游", "龙井茶文化", "灵隐与禅意", "千岛湖风光" };
        if (dest.contains("三亚"))
            return new String[] { "海滩初体验", "海岛探险", "热带雨林之旅", "美食与购物" };
        if (dest.contains("丽江"))
            return new String[] { "古城慢生活", "玉龙雪山", "泸沽湖秘境", "纳西文化体验" };
        if (dest.contains("广州"))
            return new String[] { "珠江新城", "老西关风情", "长隆欢乐游", "岭南美食之旅" };
        return new String[] { "城市初探", "深度体验", "文化之旅", "自然风光", "美食探索", "休闲购物", "周边漫游" };
    }

    private String[] getActivitiesForDestination(String dest, int day, int totalDays) {
        if (day == 1) {
            if (dest.contains("北京"))
                return new String[] { "抵达北京，办理入住", "天安门广场+故宫博物院", "前门大街品尝烤鸭", "景山公园俯瞰故宫全景", "南锣鼓巷逛街", "簋街夜市美食" };
            if (dest.contains("上海"))
                return new String[] { "抵达上海，办理入住", "南京路步行街", "上海本帮菜午餐", "外滩万国建筑群", "和平饭店下午茶", "陆家嘴夜景" };
            if (dest.contains("成都"))
                return new String[] { "抵达成都，办理入住", "宽窄巷子漫步", "地道川菜午餐", "武侯祠+锦里古街", "春熙路逛街", "九眼桥酒吧街" };
            if (dest.contains("西安"))
                return new String[] { "抵达西安，办理入住", "钟楼+鼓楼", "回民街品尝小吃", "西安城墙骑行", "大雁塔北广场", "大唐不夜城" };
            if (dest.contains("杭州"))
                return new String[] { "抵达杭州，办理入住", "断桥残雪+白堤", "楼外楼杭帮菜", "乘船游西湖", "雷峰塔观日落", "河坊街夜市" };
            if (dest.contains("三亚"))
                return new String[] { "抵达三亚，办理入住", "亚龙湾沙滩漫步", "海鲜大排档午餐", "热带天堂森林公园", "第一市场采购", "沙滩日落晚餐" };
            if (dest.contains("丽江"))
                return new String[] { "抵达丽江，办理入住", "大研古城漫步", "纳西烤鱼午餐", "木府参观", "四方街购物", "古城酒吧听民谣" };
            if (dest.contains("广州"))
                return new String[] { "抵达广州，办理入住", "沙面岛+上下九", "地道粤式茶点", "陈家祠参观", "北京路步行街", "珠江夜游" };
            return new String[] { "抵达" + dest + "，办理入住", "游览市区地标", "品尝当地特色美食", "参观热门景点", "逛街购物", "夜市或夜景体验" };
        }
        if (day == 2) {
            if (dest.contains("北京"))
                return new String[] { "前往八达岭长城", "长城徒步游览", "长城脚下农家菜", "明十三陵参观", "奥林匹克公园", "水立方+鸟巢夜景" };
            if (dest.contains("上海"))
                return new String[] { "田子坊艺术区", "艺术展览参观", "法租界西餐", "思南路老洋房", "新天地逛街", "外滩夜景游船" };
            if (dest.contains("成都"))
                return new String[] { "前往大熊猫基地", "近距离看熊猫", "成都小吃合集", "人民公园喝茶", "太古里逛街", "火锅晚餐" };
            return new String[] { "前往热门景区", "游览自然/人文景观", "景区周边午餐", "参观特色博物馆", "当地特产采购", "特色晚餐" };
        }
        if (day == totalDays) {
            if (dest.contains("北京"))
                return new String[] { "颐和园晨练", "颐和园游湖", "最后一顿烤鸭", "798艺术区", "购买伴手礼", "前往机场/车站" };
            if (dest.contains("上海"))
                return new String[] { "城隍庙+豫园", "南翔小笼包", "上海博物馆", "购买伴手礼", "最后一杯咖啡", "前往机场/车站" };
            return new String[] { "最后一天早起", "最后一个景点打卡", "告别午餐", "购买伴手礼", "整理行装", "前往机场/车站返程" };
        }
        return new String[] { "新一天出发", "游览特色景点", "品尝当地美食", "参观文化地标", "自由探索时间", "享受当地夜生活" };
    }

    private String getActivityDescription(String activity, String dest) {
        if (activity.contains("故宫"))
            return "世界最大宫殿建筑群，建议游览3小时，提前预约门票";
        if (activity.contains("长城"))
            return "世界七大奇迹之一，建议穿运动鞋，坐缆车可节省体力";
        if (activity.contains("烤鸭"))
            return dest + "必吃美食，推荐全聚德或便宜坊，人均约150元";
        if (activity.contains("西湖"))
            return "世界文化遗产，推荐乘船游湖，苏堤春晓不容错过";
        if (activity.contains("外滩"))
            return "上海地标景观，万国建筑群与陆家嘴天际线交相辉映";
        if (activity.contains("熊猫"))
            return "建议早上去，熊猫比较活跃，记得买熊猫纪念品";
        if (activity.contains("火锅"))
            return dest + "特色火锅，麻辣鲜香，推荐毛肚和鸭肠";
        if (activity.contains("兵马俑"))
            return "世界第八大奇迹，建议请讲解员，游览约3小时";
        if (activity.contains("沙滩"))
            return "细软白沙，海天一色，建议做好防晒准备";
        if (activity.contains("古城"))
            return "保存完好的古建筑群，适合慢游拍照";
        if (activity.contains("博物馆"))
            return "馆藏丰富，建议租语音导览器，游览约2小时";
        return "深度体验" + dest + "的特色，留下美好回忆";
    }
}
