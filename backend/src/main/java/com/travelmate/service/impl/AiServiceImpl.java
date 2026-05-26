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

import java.math.BigDecimal;
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

    @Autowired(required = false)
    private AttractionService attractionService;

    private boolean apiKeyWarningLogged = false;

    private static final String PLAN_SYSTEM_PROMPT = """
            你是 TravelMate 的资深中文自由行规划师。目标不是堆景点，而是生成能真实执行的行程。
            必须只返回严格 JSON，不要 markdown，不要解释文字。结构如下：
            {
              "title": "包含目的地和天数的行程标题",
              "summary": "100字以内，说明节奏、适合人群和主要亮点",
              "pace": "轻松/适中/紧凑",
              "budgetNote": "说明费用口径，必须写明是否不含往返大交通和住宿",
              "days": [
                {
                  "day": 1,
                  "date": "2026-06-01",
                  "theme": "当天主题",
                  "area": "当天主要活动区域",
                  "dayEstimatedCost": 500,
                  "tips": "当天预约、天气、体力或交通提醒",
                  "activities": [
                    {
                      "time": "09:30",
                      "name": "活动名称",
                      "description": "为什么这样安排，以及现场注意事项",
                      "type": "景点/餐饮/交通/酒店/休息/购物/娱乐",
                      "duration": "约2小时",
                      "transfer": "上一站到此约20分钟",
                      "bookingTip": "是否需要预约或购票",
                      "cost": 120
                    }
                  ]
                }
              ],
              "totalEstimatedCost": 3000
            }
            规划硬约束：
            1. 每天 3-5 个活动即可，最多 2 个重体力/长排队景点；必须安排午餐或休息缓冲。
            2. 每天尽量围绕同一区域或顺路区域组织，不要跨城折返；跨区域必须写出交通时间。
            3. 首日默认有抵达、入住或行李寄存缓冲；末日默认有返程和购物/轻量景点缓冲。
            4. 有老人、亲子、海岛、高原、雨季、热门博物馆/景区等场景时，主动降低强度并写预约/安全提醒。
            5. 预算是整团总预算。费用按人数估算，totalEstimatedCost 只统计当地门票、餐饮、市内交通和体验项目，不含往返大交通及住宿，除非用户明确要求。
            6. 优先使用用户提供的本地景点参考；没有参考时也只能使用真实常见景点，不要编造不存在的地点。
            7. 如果预算明显不足，给出低成本替代和取舍，不要硬塞高价项目。
            """;

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
        normalizePlanRequest(dto);

        String userPrompt = String.format(
                """
                        目的地：%s
                        出行天数：%d天
                        出发日期：%s
                        出行人数：%d人
                        总预算：%.0f元
                        人均每日可用预算参考：%.0f元
                        出行偏好：%s

                        本地景点参考：
                        %s

                        请按“真实可走、少折返、有缓冲”的原则规划。若景点参考不足，可以补充该目的地真实常见景点，但不要编造。
                        """,
                dto.getDestination(), dto.getDays(),
                dto.getStartDate(),
                dto.getPeopleCount(),
                dto.getBudget() != null ? dto.getBudget().doubleValue() : 0.0,
                estimatePerPersonDailyBudget(dto),
                dto.getPreferences(), buildTravelContext(dto.getDestination()));

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

    private void normalizePlanRequest(AiPlanCreateDTO dto) {
        if (dto == null) {
            throw new RuntimeException("行程参数不能为空");
        }
        String destination = dto.getDestination() == null ? "" : dto.getDestination().trim();
        if (destination.isBlank()) {
            throw new RuntimeException("请输入目的地");
        }
        dto.setDestination(destination);
        dto.setDays(clamp(dto.getDays() <= 0 ? 3 : dto.getDays(), 1, 15));
        dto.setPeopleCount(clamp(dto.getPeopleCount() <= 0 ? 1 : dto.getPeopleCount(), 1, 20));
        if (dto.getBudget() == null || dto.getBudget().doubleValue() <= 0) {
            dto.setBudget(BigDecimal.valueOf(dto.getPeopleCount() * dto.getDays() * 450L));
        }
        dto.setPreferences(dto.getPreferences() == null || dto.getPreferences().isBlank()
                ? "经典必游,美食体验,节奏适中"
                : dto.getPreferences().trim());
        dto.setStartDate(normalizeStartDate(dto.getStartDate()));
    }

    private String normalizeStartDate(String startDate) {
        if (startDate != null && !startDate.isBlank()) {
            try {
                return LocalDate.parse(startDate.trim()).toString();
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now().toString();
    }

    private double estimatePerPersonDailyBudget(AiPlanCreateDTO dto) {
        return dto.getBudget().doubleValue() / Math.max(dto.getPeopleCount(), 1) / Math.max(dto.getDays(), 1);
    }

    private String buildTravelContext(String destination) {
        if (attractionService == null) {
            return "暂无本地景点库参考，请使用真实常见景点并控制同日距离。";
        }
        try {
            List<Attraction> attractions = attractionService.searchAttractions(destination);
            if ((attractions == null || attractions.isEmpty()) && destination.endsWith("市")) {
                attractions = attractionService.searchAttractions(destination.substring(0, destination.length() - 1));
            }
            if (attractions == null || attractions.isEmpty()) {
                return "暂无本地景点库参考，请使用真实常见景点并控制同日距离。";
            }
            StringBuilder context = new StringBuilder();
            int limit = Math.min(attractions.size(), 8);
            for (int i = 0; i < limit; i++) {
                Attraction a = attractions.get(i);
                context.append(i + 1).append(". ")
                        .append(a.getName());
                if (a.getAddress() != null && !a.getAddress().isBlank()) {
                    context.append("，地址：").append(a.getAddress());
                }
                if (a.getOpenTime() != null && !a.getOpenTime().isBlank()) {
                    context.append("，开放时间：").append(a.getOpenTime());
                }
                if (a.getAdultPrice() != null) {
                    context.append("，成人票：").append(a.getAdultPrice()).append("元");
                }
                context.append("\n");
            }
            return context.toString().trim();
        } catch (Exception e) {
            log.debug("读取本地景点参考失败: {}", e.getMessage());
            return "暂无本地景点库参考，请使用真实常见景点并控制同日距离。";
        }
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
                String normalized = normalizePlanContent(content);
                if (normalized != null) {
                    log.info("AI 行程生成成功");
                    return normalized;
                }
            }
            log.warn("AI 返回内容结构不完整，使用降级模板");
        } catch (Exception e) {
            log.warn("AI 行程生成失败: {}，使用降级模板", e.getMessage());
        }
        return generateFallbackPlan(dto);
    }

    private String normalizePlanContent(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode days = root.path("days");
            if (!days.isArray() || days.isEmpty()) {
                return null;
            }
            for (JsonNode day : days) {
                JsonNode activities = day.path("activities");
                if (!activities.isArray() || activities.isEmpty()) {
                    return null;
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("AI 行程 JSON 校验失败: {}", e.getMessage());
            return null;
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
            case "low" -> "low";
            case "medium" -> "medium";
            case "high" -> "high";
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
        int days = dto.getDays();
        int people = Math.max(dto.getPeopleCount(), 1);
        double budget = dto.getBudget() != null ? dto.getBudget().doubleValue() : 5000;
        String prefs = dto.getPreferences() != null ? dto.getPreferences() : "";
        LocalDate startDate = LocalDate.parse(normalizeStartDate(dto.getStartDate()));

        StringBuilder json = new StringBuilder();
        json.append("{\"title\":\"").append(escapeJson(dest)).append(" ").append(days).append("日松弛自由行\",");
        json.append("\"summary\":\"")
                .append(escapeJson("AI服务暂时不可用，先给出一份按同区顺路、每天留缓冲的" + dest + days + "日行程。费用按" + people + "人估算，不含往返大交通和住宿。"))
                .append("\",");
        json.append("\"pace\":\"适中\",");
        json.append("\"budgetNote\":\"费用仅估算当地门票、餐饮、市内交通和体验项目，不含往返大交通及住宿。\",");
        json.append("\"days\":[");

        double totalCost = 0;
        for (int d = 1; d <= days; d++) {
            if (d > 1)
                json.append(",");
            DayPlan dayPlan = buildDayPlan(dest, d, days, prefs, people, budget / days, startDate.plusDays(d - 1));
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

    private DayPlan buildDayPlan(String dest, int day, int totalDays, String prefs, int people, double dayBudget,
            LocalDate date) {
        String[] themes = getThemesForDestination(dest);
        String theme = themes[(day - 1) % themes.length];

        StringBuilder sb = new StringBuilder();
        sb.append("{\"day\":").append(day).append(",\"theme\":\"").append(escapeJson(theme))
                .append("\",\"date\":\"").append(date).append("\",");
        sb.append("\"area\":\"").append(escapeJson(getAreaHint(dest, day))).append("\",");
        sb.append("\"tips\":\"").append(escapeJson(buildDayTip(dest, prefs, day, totalDays))).append("\",");
        sb.append("\"activities\":[");

        double dayCost = 0;
        String[] activities = getActivitiesForDestination(dest, day, totalDays);
        String[] times = day == 1
                ? new String[] { "10:30", "12:30", "15:00", "18:30" }
                : day == totalDays
                        ? new String[] { "09:30", "11:00", "13:00", "15:30" }
                        : new String[] { "09:00", "12:00", "14:30", "18:30" };
        String[] types = day == 1
                ? new String[] { "交通", "餐饮", "景点", "餐饮" }
                : day == totalDays
                        ? new String[] { "酒店", "景点", "餐饮", "交通" }
                        : new String[] { "景点", "餐饮", "景点", "餐饮" };
        int[] baseCosts = day == 1
                ? new int[] { 30, 70, 90, 90 }
                : day == totalDays
                        ? new int[] { 0, 80, 70, 35 }
                        : new int[] { 120, 75, 90, 95 };
        String[] durations = { "约1小时", "约1小时", "约2小时", "约1.5小时" };
        String[] transfers = { "从住宿或交通枢纽出发", "上一站到此约15-25分钟", "午后留30分钟缓冲", "傍晚避开高峰出行" };

        for (int i = 0; i < activities.length; i++) {
            if (i > 0)
                sb.append(",");
            int cost = estimateActivityCost(baseCosts[i], people, dayBudget);
            if (baseCosts[i] > 0 && cost < 10)
                cost = 10;
            dayCost += cost;
            sb.append("{\"time\":\"").append(times[i]).append("\",");
            sb.append("\"name\":\"").append(escapeJson(activities[i])).append("\",");
            sb.append("\"description\":\"").append(escapeJson(getActivityDescription(activities[i], dest)))
                    .append("\",");
            sb.append("\"type\":\"").append(types[i]).append("\",");
            sb.append("\"duration\":\"").append(durations[i]).append("\",");
            sb.append("\"transfer\":\"").append(escapeJson(transfers[i])).append("\",");
            sb.append("\"bookingTip\":\"").append(escapeJson(getBookingTip(activities[i]))).append("\",");
            sb.append("\"cost\":").append(cost).append("}");
        }

        sb.append("],\"dayEstimatedCost\":").append((int) dayCost).append("}");
        return new DayPlan(sb.toString(), dayCost);
    }

    private int estimateActivityCost(int basePerPerson, int people, double dayBudget) {
        if (basePerPerson <= 0) {
            return 0;
        }
        double dayReference = Math.max(people, 1) * 420.0;
        double scale = clampDouble(dayBudget / dayReference, 0.45, 1.25);
        return (int) Math.round(basePerPerson * Math.max(people, 1) * scale);
    }

    private String getAreaHint(String dest, int day) {
        if (dest.contains("北京")) return day == 1 ? "天安门-故宫-前门片区" : day == 2 ? "八达岭长城方向" : "市区顺路片区";
        if (dest.contains("上海")) return day == 1 ? "南京路-外滩-陆家嘴" : "法租界/老城厢片区";
        if (dest.contains("成都")) return day == 1 ? "宽窄巷子-武侯祠-锦里" : "熊猫基地-市区茶馆";
        if (dest.contains("西安")) return day == 1 ? "钟鼓楼-城墙-大雁塔" : "临潼/市区历史线";
        if (dest.contains("三亚")) return day == 1 ? "酒店海湾周边" : "离岛或雨林方向";
        return "同区顺路安排";
    }

    private String buildDayTip(String dest, String prefs, int day, int totalDays) {
        if (day == 1) {
            return "首日按抵达和入住留缓冲，不建议排需要早起预约的重景点。";
        }
        if (day == totalDays) {
            return "末日控制在半日轻量活动，返程前至少预留2小时机动时间。";
        }
        if (prefs.contains("亲子")) {
            return "亲子行程建议午后安排休息，晚间不超过21:00返回酒店。";
        }
        if (dest.contains("丽江") || dest.contains("云南")) {
            return "高原地区避免连续剧烈活动，注意保暖、防晒和补水。";
        }
        if (dest.contains("三亚")) {
            return "海岛行程看天气调整，防晒、防水袋和换洗衣物要提前准备。";
        }
        return "当天景点集中在相邻片区，午后留出排队和交通缓冲。";
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
                return new String[] { "抵达北京，办理入住或寄存行李", "前门大街简餐", "天安门广场+故宫博物院", "景山公园或前门烤鸭二选一" };
            if (dest.contains("上海"))
                return new String[] { "抵达上海，办理入住或寄存行李", "南京路附近本帮菜午餐", "外滩万国建筑群慢走", "陆家嘴夜景或黄浦江轮渡" };
            if (dest.contains("成都"))
                return new String[] { "抵达成都，办理入住或寄存行李", "宽窄巷子简餐", "武侯祠+锦里古街", "春熙路或太古里轻松晚餐" };
            if (dest.contains("西安"))
                return new String[] { "抵达西安，办理入住或寄存行李", "钟鼓楼周边午餐", "西安城墙轻量骑行", "大唐不夜城夜游" };
            if (dest.contains("杭州"))
                return new String[] { "抵达杭州，办理入住或寄存行李", "湖滨或白堤附近午餐", "西湖白堤+断桥慢走", "河坊街晚餐和夜逛" };
            if (dest.contains("三亚"))
                return new String[] { "抵达三亚，办理入住", "酒店周边海鲜午餐", "亚龙湾沙滩适应节奏", "沙滩日落晚餐" };
            if (dest.contains("丽江"))
                return new String[] { "抵达丽江，办理入住", "纳西风味午餐", "大研古城慢走", "四方街周边晚餐" };
            if (dest.contains("广州"))
                return new String[] { "抵达广州，办理入住", "地道粤式茶点午餐", "沙面岛+上下九慢走", "珠江夜游或北京路晚餐" };
            return new String[] { "抵达" + dest + "，办理入住或寄存行李", "品尝当地特色午餐", "游览市区地标", "夜市或夜景轻体验" };
        }
        if (day == 2) {
            if (dest.contains("北京"))
                return new String[] { "前往八达岭长城", "长城徒步或缆车游览", "长城脚下简餐", "返城后奥林匹克公园夜景" };
            if (dest.contains("上海"))
                return new String[] { "武康路和安福路citywalk", "法租界简餐", "思南路老洋房慢走", "新天地或外滩夜景" };
            if (dest.contains("成都"))
                return new String[] { "前往大熊猫基地", "看熊猫并避开午后人流", "成都小吃午餐", "人民公园喝茶+火锅晚餐" };
            return new String[] { "前往核心景区", "游览自然或人文景观", "景区周边午餐", "回到住宿片区特色晚餐" };
        }
        if (day == totalDays) {
            if (dest.contains("北京"))
                return new String[] { "退房并寄存行李", "颐和园或恭王府二选一", "最后一顿烤鸭或京味午餐", "前往机场/车站" };
            if (dest.contains("上海"))
                return new String[] { "退房并寄存行李", "豫园或上海博物馆二选一", "南翔小笼包或本帮菜午餐", "前往机场/车站" };
            return new String[] { "退房并寄存行李", "最后一个轻量景点", "告别午餐和伴手礼", "前往机场/车站返程" };
        }
        return new String[] { "上午游览特色景点", "品尝当地午餐", "下午参观文化地标或自然景观", "住宿片区附近轻松晚餐" };
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

    private String getBookingTip(String activity) {
        if (activity.contains("故宫") || activity.contains("博物馆") || activity.contains("长城")
                || activity.contains("熊猫") || activity.contains("玉龙雪山")) {
            return "建议提前预约或购票，并确认当天开放时间";
        }
        if (activity.contains("退房") || activity.contains("抵达") || activity.contains("机场") || activity.contains("车站")) {
            return "预留行李寄存和交通缓冲";
        }
        if (activity.contains("晚餐") || activity.contains("烤鸭") || activity.contains("火锅")) {
            return "热门餐厅建议提前取号或预约";
        }
        return "按当天客流和天气灵活调整";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
