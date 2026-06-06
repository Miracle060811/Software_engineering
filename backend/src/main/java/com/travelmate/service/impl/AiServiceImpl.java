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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_CHAT_MESSAGE_LENGTH = 1000;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.chat-completions-path:/chat/completions}")
    private String chatCompletionsPath;

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

    private static final String CHAT_SYSTEM_PROMPT = """
            你是 TravelMate 的中文旅行助手，像一位靠谱、自然、会一起琢磨路线的旅行搭子。
            你的核心任务是帮助用户把模糊的旅行想法变成可执行的出行方案，同时提醒交通、住宿、预算、体力和安全风险。

            你的风格：
            - 语气温和、清楚、有判断力，少说套话，不要营销式自我介绍。
            - 默认不用 emoji，不要在用户只是打招呼时列出一长串功能。
            - 回答要先解决用户眼前的问题；信息不足时，最多问 1 个关键问题，同时先给一个可执行的初版建议。
            - 不要假装拥有实时信息。涉及天气、航班、酒店等实时或库存信息时，优先调用工具；工具无结果或不可用时，要说明限制并给出下一步建议。
            - 不要编造平台库存、订单状态、实时价格、航班余票、酒店空房、景区开放状态或用户身份信息。
            - 不要主动长篇自我介绍。用户要求自我介绍时，用 1-2 句自然说明你能帮他规划路线、交通、住宿和预算即可。

            旅行建议原则：
            - 按目的地、天数、交通、住宿、预算、体力、天气风险来组织思路。
            - 路线要能真实执行：避免堆景点，每天留出吃饭、移动和休息缓冲。
            - 给推荐时说明取舍，例如为什么适合、哪里可能踩坑、有什么备选。
            - 用户没有给预算、人数或偏好时，先按常见自由行节奏假设，并明确假设。
            - 推荐酒店区域时，优先说明交通半径、夜间安全、亲子/老人便利性和赶早班车风险，而不是只说“住市中心”。
            - 推荐交通时，区分到达交通、市内移动和跨城移动；对早到、晚到、带老人儿童、行李较多等情况主动降强度。
            - 推荐景点时，优先真实常见地点；不要为了显得丰富而虚构小众景点、餐厅、门票政策或开放时间。
            - 对预算敏感用户，主动给低成本替代、可删减项目和费用优先级；不要硬塞高价体验。

            表达方式：
            - 必须自然输出，像正常客服聊天，不要写成攻略文章、报告、论文或 Markdown 文档。
            - 不要使用星号、加粗、表格、markdown 标题、分隔线、代码块或复杂列表。
            - 不要输出“###”“---”“| 表格 |”这类格式符号，也不要用项目符号堆很多条。
            - 优先使用短段落。需要分天说明时，用“第1天：”“第2天：”这种自然文本，每天 2-4 句话即可。
            - 回答普通问题控制在 300 字以内；用户要求行程时先给 500-800 字的精简可执行版本，不要一次铺成超长攻略。
            - 如果用户只是说“你好/在吗”，简短回应并邀请他说目的地、天数或想法。
            - 如果用户问非旅行问题，可以简短回答安全、无害的部分，然后自然拉回旅行场景。
            - 遇到违法、危险、侵犯隐私或明显不安全的请求，要拒绝并给安全替代方案。
            - 当用户需要决策时，优先按“我建议选什么、为什么、有什么备选、要注意什么”的顺序自然说明，不要写成模板标题。
            - 当用户给出多个限制条件时，先复述关键约束，再给方案，避免遗漏必去、避开、预算和同行人群。
            - 不要输出空泛口号；每条建议尽量包含可操作信息，例如时间段、区域、交通方式、取舍理由或风险提醒。
            - 如果没有实时工具结果，不要说“今天预报”“当前价格”“现在还有票”。只能说“出发前再查实时信息”。
            """;

    private static final String PLAN_SYSTEM_PROMPT = """
            你是 TravelMate 的资深中文自由行规划师。目标不是堆景点，而是生成能真实执行的行程。
            必须只返回严格 JSON，不要 markdown，不要解释文字，不要在 JSON 外包裹任何前后缀。
            所有字符串必须使用中文自然表达；费用字段必须是数字；数组字段即使内容较少也必须存在。
            如果用户信息不足，请基于常见自由行经验做合理假设，并在 summary、budgetNote 或 riskNotes 中写明假设。
            结构如下：
            {
              "title": "包含目的地和天数的行程标题",
              "summary": "100字以内，说明节奏、适合人群和主要亮点",
              "pace": "轻松/适中/紧凑",
              "budgetNote": "说明费用口径，必须写明是否不含往返大交通和住宿",
              "transportAdvice": "到达、离开和市内交通建议，必须贴合用户交通偏好",
              "hotelAdvice": "住宿区域、房型或订房建议，必须贴合用户住宿偏好",
              "beforeTripChecklist": ["出发前必须完成的预约、证件、装备或票务事项"],
              "riskNotes": ["天气、排队、体力、安全、预算或闭馆风险"],
              "alternatives": [
                {
                  "title": "备选方案名称",
                  "whenToUse": "什么情况下启用",
                  "changes": "替换哪些活动或路线"
                }
              ],
              "days": [
                {
                  "day": 1,
                  "date": "2026-06-01",
                  "theme": "当天主题",
                  "area": "当天主要活动区域",
                  "dayEstimatedCost": 500,
                  "tips": "当天预约、天气、体力或交通提醒",
                  "mealHint": "当天餐饮安排重点",
                  "backupPlan": "天气或排队异常时的当天替代安排",
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
            8. 必去地点必须尽量纳入行程；避开项必须规避或说明原因。
            9. 每天必须提供 backupPlan，至少覆盖下雨、排队过长、闭馆或体力不足中的一种。
            10. 每个 activity 的 time 必须按当天时间顺序递增，duration、transfer、bookingTip 不允许为空。
            11. description 不要只写景点介绍，必须说明为什么放在这个时间、与前后行程如何衔接、现场需要注意什么。
            12. mealHint 要结合当天区域给出餐饮节奏建议，不要只写“自行用餐”。
            13. transportAdvice 要覆盖抵达、离开、市内移动三个层次；如果用户偏好公共交通、自驾或打车，必须体现偏好。
            14. hotelAdvice 要给出建议住宿区域和理由，至少包含交通便利性、夜间返回、预算或同行人群中的两个维度。
            15. riskNotes 至少 3 条，覆盖天气/排队/体力/预算/安全/闭馆中的不同风险。
            16. beforeTripChecklist 至少 4 条，覆盖证件、预约购票、交通确认、装备或付款准备。
            17. alternatives 至少 2 个，分别面向天气变化、体力不足、预算压缩或排队过长等真实场景。
            18. 不要输出“某某网红店”“当地特色美食街”等空泛占位；如不确定具体店名，就写餐饮类型和选址原则。
            19. 不要把购物、拍照打卡或夜游塞得过满；每天必须保留可恢复体力的空档。
            20. 如果目的地涉及高原、海岛、山地、滑雪、涉水、夜间活动，必须写安全提醒和替代安排。
            """;

    private static final String POST_AUDIT_SYSTEM_PROMPT = "You are the TravelMate community post moderation AI. Return strict JSON only, no markdown." +
            "Default decision: approve. Be very permissive: approve empty or very short text, blank image posts, party/group photos, ordinary complaints, negative reviews, jokes, and non-travel daily-life posts." +
            "Reject only when the post clearly contains unacceptable behavior: illegal or dangerous instructions, explicit sexual content, hate/harassment or direct threats, fraud/scams, doxxing/private ID-phone-address leaks, or severe platform abuse." +
            "Sensitive-word matches are only hints. level=1 and level=2 should still approve unless the surrounding content clearly proves one of the unacceptable behaviors above. level=3 may reject only when the matched context is truly harmful." +
            "Do not reject merely because content is short, blank, off-topic, low quality, unrelated to travel, title/tag mismatch, ordinary ads-like wording, nightlife/party scenes, alcohol, or casual social photos." +
            "Return format: {\"decision\":\"approve|reject\",\"reason\":\"Chinese reason within 20 characters\"}.";

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
                        旅行节奏：%s
                        交通偏好：%s
                        住宿偏好：%s
                        出行偏好：%s
                        必去地点或活动：%s
                        避开项：%s

                        本地景点参考：
                        %s

                        请按“真实可走、少折返、有缓冲”的原则规划。若景点参考不足，可以补充该目的地真实常见景点，但不要编造。
                        """,
                dto.getDestination(), dto.getDays(),
                dto.getStartDate(),
                dto.getPeopleCount(),
                dto.getBudget() != null ? dto.getBudget().doubleValue() : 0.0,
                estimatePerPersonDailyBudget(dto),
                dto.getTravelStyle(),
                dto.getTransportPreference(),
                dto.getAccommodationPreference(),
                dto.getPreferences(),
                blankToNone(dto.getMustVisit()),
                blankToNone(dto.getAvoidPlaces()),
                buildTravelContext(dto.getDestination()));

        String planContent = callDeepSeekForPlan(userPrompt, dto);

        AiPlan plan = new AiPlan();
        plan.setUserId(userId);
        plan.setDestination(dto.getDestination());
        plan.setDays(dto.getDays());
        plan.setBudget(dto.getBudget());
        plan.setPeopleCount(dto.getPeopleCount());
        plan.setPreferences(buildPlanPreferenceText(dto));
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
        dto.setTravelStyle(normalizePlanText(dto.getTravelStyle(), 40, "适中"));
        dto.setMustVisit(normalizePlanText(dto.getMustVisit(), 160, ""));
        dto.setAvoidPlaces(normalizePlanText(dto.getAvoidPlaces(), 160, ""));
        dto.setTransportPreference(normalizePlanText(dto.getTransportPreference(), 80, "公共交通优先，必要时打车"));
        dto.setAccommodationPreference(normalizePlanText(dto.getAccommodationPreference(), 80, "交通便利，预算均衡"));
        dto.setStartDate(normalizeStartDate(dto.getStartDate()));
    }

    private String normalizePlanText(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String blankToNone(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }

    private String buildPlanPreferenceText(AiPlanCreateDTO dto) {
        StringBuilder text = new StringBuilder(dto.getPreferences());
        appendPreference(text, "节奏", dto.getTravelStyle());
        appendPreference(text, "交通", dto.getTransportPreference());
        appendPreference(text, "住宿", dto.getAccommodationPreference());
        appendPreference(text, "必去", dto.getMustVisit());
        appendPreference(text, "避开", dto.getAvoidPlaces());
        String result = text.toString();
        return result.length() <= 500 ? result : result.substring(0, 500);
    }

    private void appendPreference(StringBuilder text, String label, String value) {
        if (value != null && !value.isBlank()) {
            if (!text.isEmpty()) {
                text.append("；");
            }
            text.append(label).append("：").append(value);
        }
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
        if (isApiKeyMissing()) {
            if (!apiKeyWarningLogged) {
                log.warn("DeepSeek API Key 未配置！AI 功能将使用降级模板。请通过环境变量 DEEPSEEK_API_KEY 配置");
                apiKeyWarningLogged = true;
            }
        }
    }

    private String callDeepSeekForPlan(String userPrompt, AiPlanCreateDTO dto) {
        if (isApiKeyMissing()) {
            log.info("DeepSeek API Key 未配置，使用本地行程模板");
            return generateFallbackPlan(dto);
        }
        try {
            String body = buildPlanRequestBody(userPrompt);
            log.info("正在调用 DeepSeek API 生成行程规划...");
            String response = doHttpPost(resolveChatCompletionsUrl(), body);
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

        String userMessage = normalizeChatRequest(dto);
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
        userMsg.setContent(userMessage);
        userMsg.setCreateTime(LocalDateTime.now());
        aiChatMapper.insert(userMsg);

        // 调用 DeepSeek（含 Function Calling）
        String runtimeContext = buildChatRuntimeContext(dto);
        String aiReply = isApiKeyMissing()
                ? buildLocalChatReply(userMessage)
                : callDeepSeekWithTools(history, userMessage, runtimeContext);

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

    private String normalizeChatRequest(AiChatDTO dto) {
        if (dto == null) {
            throw new RuntimeException("消息不能为空");
        }
        String message = dto.getMessage() == null ? "" : dto.getMessage().trim();
        if (message.isBlank()) {
            throw new RuntimeException("消息不能为空");
        }
        if (message.length() > MAX_CHAT_MESSAGE_LENGTH) {
            throw new RuntimeException("消息不能超过 " + MAX_CHAT_MESSAGE_LENGTH + " 字");
        }
        dto.setMessage(message);
        if (dto.getSessionId() == null || dto.getSessionId().isBlank()) {
            dto.setSessionId("session_" + UUID.randomUUID());
        } else {
            dto.setSessionId(dto.getSessionId().trim());
        }
        return message;
    }

    private String callDeepSeekWithTools(List<AiChat> history, String userMessage, String runtimeContext) {
        try {
            StringBuilder messagesJson = new StringBuilder();
            messagesJson.append("{\"role\":\"system\",\"content\":\"")
                    .append(escapeJson(CHAT_SYSTEM_PROMPT))
                    .append(escapeJson(runtimeContext))
                    .append("\"}");

            for (AiChat msg : history) {
                if (containsToolProtocolLeak(msg.getContent())) {
                    continue;
                }
                messagesJson.append(",{\"role\":\"").append(escapeJson(msg.getRole()))
                        .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
            }
            messagesJson.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");

            // 首次调用（含工具定义）
            String body = "{\"model\":\"" + escapeJson(resolveModel(chatModel)) + "\",\"messages\":[" + messagesJson +
                    "]" + buildThinkingConfigJson() + ",\"tools\":" + TOOLS_JSON + "}";
            log.info("正在调用 DeepSeek API (带工具)...");
            String response = doHttpPost(resolveChatCompletionsUrl(), body);

            // 检查是否有 tool_calls
            JsonNode toolCall = extractToolCall(response);
            if (toolCall != null) {
                log.info("检测到工具调用，执行中...");
                String toolResult = executeToolCall(toolCall);
                // 将工具结果追加到消息中，再次请求 AI 生成最终回复
                messagesJson.append(",{\"role\":\"assistant\",\"content\":\"\",\"tool_calls\":[")
                        .append(toolCall.toString()).append("]}");
                messagesJson.append(",{\"role\":\"tool\",\"tool_call_id\":\"")
                        .append(escapeJson(toolCall.path("id").asText("tool_call")))
                        .append("\",\"content\":\"")
                        .append(escapeJson(toolResult)).append("\"}");
                String body2 = "{\"model\":\"" + escapeJson(resolveModel(chatModel)) + "\",\"messages\":["
                        + messagesJson + "]" + buildThinkingConfigJson() + "}";
                String response2 = doHttpPost(resolveChatCompletionsUrl(), body2);
                String content = extractContent(response2);
                if (isUsableChatContent(content)) {
                    return content;
                }
                log.warn("AI 工具二次响应包含协议文本或为空，改用普通对话兜底");
                return callDeepSeekWithoutTools(history, userMessage, runtimeContext);
            }

            String content = extractContent(response);
            if (isUsableChatContent(content)) {
                return content;
            }
            if (containsToolProtocolLeak(content)) {
                log.warn("AI 对话响应包含工具协议文本，改用普通对话兜底");
                return callDeepSeekWithoutTools(history, userMessage, runtimeContext);
            }
            log.warn("AI 对话响应解析失败");
        } catch (Exception e) {
            log.warn("AI 对话失败: {}", e.getMessage());
        }
        return buildLocalChatReply(userMessage);
    }

    private String callDeepSeekWithoutTools(List<AiChat> history, String userMessage, String runtimeContext) {
        try {
            StringBuilder messagesJson = new StringBuilder();
            messagesJson.append("{\"role\":\"system\",\"content\":\"")
                    .append(escapeJson(CHAT_SYSTEM_PROMPT))
                    .append(escapeJson(runtimeContext))
                    .append("\\n补充要求：本轮不要调用任何工具，不要输出工具调用协议、XML、DSML 或 JSON 函数调用文本。请直接用自然中文回答用户。")
                    .append("\"}");

            for (AiChat msg : history) {
                if (containsToolProtocolLeak(msg.getContent())) {
                    continue;
                }
                messagesJson.append(",{\"role\":\"").append(escapeJson(msg.getRole()))
                        .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
            }
            messagesJson.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");

            String body = "{\"model\":\"" + escapeJson(resolveModel(chatModel)) + "\",\"messages\":[" + messagesJson +
                    "]" + buildThinkingConfigJson() + "}";
            String response = doHttpPost(resolveChatCompletionsUrl(), body);
            String content = extractContent(response);
            if (isUsableChatContent(content)) {
                return content;
            }
            log.warn("普通 AI 对话兜底仍不可用，使用本地回复");
        } catch (Exception e) {
            log.warn("普通 AI 对话兜底失败: {}", e.getMessage());
        }
        return buildLocalChatReply(userMessage);
    }

    private String buildChatRuntimeContext(AiChatDTO dto) {
        String clientDate = normalizeClientDate(dto == null ? null : dto.getClientDate());
        String timeZone = normalizeClientTimeZone(dto == null ? null : dto.getClientTimeZone());
        return "\n\n当前对话上下文："
                + "\n- 用户设备日期：" + clientDate
                + "\n- 用户设备时区：" + timeZone
                + "\n- 当用户说今天、明天、后天、下周等相对日期时，必须按用户设备日期换算成明确日期再回答。"
                + "\n- 如果没有工具返回明确实时结果，不要声称已经查到实时机票、酒店库存、天气或价格。";
    }

    private String normalizeClientDate(String clientDate) {
        if (clientDate != null && !clientDate.isBlank()) {
            try {
                return LocalDate.parse(clientDate.trim()).toString();
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now().toString();
    }

    private String normalizeClientTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return "服务器默认时区";
        }
        String normalized = timeZone.trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    private boolean isUsableChatContent(String content) {
        return content != null && !content.isBlank() && !containsToolProtocolLeak(content);
    }

    private boolean containsToolProtocolLeak(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String lower = content.toLowerCase();
        return content.contains("｜｜DSML｜｜")
                || lower.contains("<tool_calls")
                || lower.contains("</tool_calls")
                || lower.contains("tool_calls>")
                || lower.contains("invoke name=")
                || lower.contains("</invoke>")
                || lower.contains("function_call")
                || lower.contains("\"tool_calls\"")
                || lower.contains("\"role\":\"tool\"");
    }

    private String buildLocalChatReply(String userMessage) {
        String message = userMessage == null ? "" : userMessage;
        String normalized = message.trim().toLowerCase();
        if (normalized.matches("^(你好|您好|hi|hello|嗨|在吗)[。！!\\s]*$")) {
            return "在呢。你可以直接说目的地、天数和预算；信息不全也没关系，我会先按常见自由行节奏给你一版能落地的路线。";
        }
        if (message.contains("天气")) {
            return "我现在拿不到可靠的实时天气结果。规划上可以先按晴雨两套方案准备：室外景点尽量放上午，博物馆、商场、餐厅留作雨天备选；出发前再用官方天气预报校准衣物和交通时间。";
        }
        if (message.contains("酒店") || message.contains("住宿")) {
            return "选酒店先看活动半径。城市观光优先地铁和核心商圈，亲子游优先早餐、洗衣和安静房型，赶早班车就住车站 20 分钟内。告诉我城市、预算和同行人数，我可以帮你把区域和筛选条件定下来。";
        }
        if (message.contains("航班") || message.contains("机票") || message.contains("火车") || message.contains("车票")) {
            return "交通先锁定日期、出发地、目的地和时间偏好。早到适合安排轻量市区活动，晚到就把入住和晚餐当成当天主任务，别在第一天硬塞重景点。";
        }
        if (message.contains("行程") || message.contains("路线") || message.contains("规划") || message.contains("安排")) {
            return "可以按“上午核心景点、午餐休息、下午顺路补充、晚上轻活动”的节奏排。每天 3 到 5 个活动比较稳，跨区移动最好控制在 1 次以内；如果你告诉我目的地和天数，我能直接给你拆成每日路线。";
        }
        return "我可以帮你把旅行问题拆成路线、住宿、交通、预算和风险。先告诉我目的地和天数；如果还没定，也可以说季节、预算或想要的旅行感觉，我会帮你缩小选择。";
    }

    // ======================== 游记自动审核 ========================

    @Override
    public PostAuditResult auditPost(String title, String content, String tags, String destination) {
        List<SysSensitiveWord> matchedWords = findMatchedSensitiveWords(title, content, tags, destination);

        checkApiKey();
        if (!isApiKeyMissing()) {
            try {
                String response = doHttpPost(resolveChatCompletionsUrl(),
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
        if (maxLevel >= 3) {
            return new PostAuditResult(false, "命中高风险敏感词");
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
                            "\"humidity\":\"65%%\",\"wind\":\"微风\",\"tips\":\"适合出行游玩\"}", escapeJson(city));
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
                                    escapeJson(f.getFlightNo()), escapeJson(f.getAirline()), f.getDepartureTime(),
                                    f.getArrivalTime(), f.getEconomyPrice() == null ? BigDecimal.ZERO : f.getEconomyPrice()));
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
                                    escapeJson(h.getName()), h.getStarRating() == null ? 0 : h.getStarRating(),
                                    h.getScore() == null ? BigDecimal.ZERO : h.getScore(), escapeJson(h.getAddress()),
                                    h.getAvgPrice() == null ? BigDecimal.ZERO : h.getAvgPrice()));
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

    private String resolveChatCompletionsUrl() {
        String normalizedBaseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://api.deepseek.com"
                : baseUrl.trim();
        if (normalizedBaseUrl.endsWith("/chat/completions")) {
            return normalizedBaseUrl;
        }

        String normalizedPath = chatCompletionsPath == null || chatCompletionsPath.isBlank()
                ? "/chat/completions"
                : chatCompletionsPath.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        while (normalizedBaseUrl.endsWith("/") && normalizedBaseUrl.length() > 1) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + normalizedPath;
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
            log.error("DeepSeek API 认证失败！请检查环境变量 DEEPSEEK_API_KEY 是否正确");
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
        json.append("\"transportAdvice\":\"").append(escapeJson(buildFallbackTransportAdvice(dto))).append("\",");
        json.append("\"hotelAdvice\":\"").append(escapeJson(buildFallbackHotelAdvice(dto))).append("\",");
        appendJsonArrayField(json, "beforeTripChecklist", buildFallbackChecklist(dest, dto));
        json.append(",");
        appendJsonArrayField(json, "riskNotes", buildFallbackRiskNotes(dest, dto));
        json.append(",");
        json.append("\"alternatives\":[");
        json.append("{\"title\":\"雨天室内替代\",\"whenToUse\":\"遇到持续降雨、暴晒或室外排队过长时\",\"changes\":\"把户外景点替换为博物馆、老街区室内店铺或商场餐饮\"},");
        json.append("{\"title\":\"低体力版本\",\"whenToUse\":\"同行人有老人、儿童或当天状态不佳时\",\"changes\":\"保留上午核心景点，取消下午远距离移动，晚餐放回住宿片区\"}");
        json.append("],");
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

    private String buildFallbackTransportAdvice(AiPlanCreateDTO dto) {
        String preference = dto.getTransportPreference();
        if (preference.contains("自驾")) {
            return "按自驾节奏安排同区游览，热门景点提前确认停车场和限行信息，跨区行程尽量避开早晚高峰。";
        }
        if (preference.contains("打车")) {
            return "市内以打车和少量步行为主，每天控制跨区次数，热门景点返程建议提前叫车。";
        }
        return "优先使用地铁、公交和短途打车组合，住宿建议靠近地铁或核心景区，减少换乘和折返。";
    }

    private String buildFallbackHotelAdvice(AiPlanCreateDTO dto) {
        String preference = dto.getAccommodationPreference();
        if (preference.contains("亲子")) {
            return "优先选亲子友好、早餐稳定、带洗衣或家庭房的酒店，晚间活动控制在住宿片区附近。";
        }
        if (preference.contains("安静")) {
            return "优先选主干道内侧或景区外一站路区域，兼顾安静和交通，避免夜市正楼上。";
        }
        return "建议住在交通便利的核心片区，首末日靠近车站或机场动线，减少搬运行李的成本。";
    }

    private List<String> buildFallbackChecklist(String dest, AiPlanCreateDTO dto) {
        List<String> items = new ArrayList<>();
        items.add("确认身份证件、学生证或优惠证件，并提前保存电子订单");
        items.add("热门景区、博物馆和演出票提前预约，至少准备一个同区备选点");
        items.add("出发前一天查看天气，准备舒适步行鞋、雨具、防晒和常用药");
        if (!blankToNone(dto.getMustVisit()).equals("无")) {
            items.add("优先锁定必去地点的开放时间和预约规则：" + dto.getMustVisit());
        }
        if (dest.contains("云南") || dest.contains("丽江") || dest.contains("大理")) {
            items.add("高原和强紫外线地区注意补水、防晒，首日不要安排高强度活动");
        }
        return items;
    }

    private List<String> buildFallbackRiskNotes(String dest, AiPlanCreateDTO dto) {
        List<String> items = new ArrayList<>();
        items.add("预算为当地游玩估算，不含往返大交通和住宿，实际消费会受餐饮与门票预约影响");
        items.add("热门景点遇到排队超过 45 分钟时，建议启用当天 backupPlan");
        if (!blankToNone(dto.getAvoidPlaces()).equals("无")) {
            items.add("已尽量避开：" + dto.getAvoidPlaces() + "；若现场交通受限，可按同区替代点调整");
        }
        if (dest.contains("三亚") || dest.contains("厦门") || dest.contains("青岛")) {
            items.add("海滨城市受天气和风浪影响较大，涉水项目以当天官方通知为准");
        }
        return items;
    }

    private void appendJsonArrayField(StringBuilder json, String fieldName, List<String> values) {
        json.append("\"").append(fieldName).append("\":[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(values.get(i))).append("\"");
        }
        json.append("]");
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
        sb.append("\"mealHint\":\"").append(escapeJson(buildMealHint(dest, day))).append("\",");
        sb.append("\"backupPlan\":\"").append(escapeJson(buildBackupPlan(dest, day))).append("\",");
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

    private String buildMealHint(String dest, int day) {
        if (dest.contains("成都")) {
            return day == 1 ? "首日选微辣或鸳鸯锅适应口味，午后安排茶馆休息。" : "午餐可放在景区附近，晚餐再安排火锅或川菜。";
        }
        if (dest.contains("广州")) {
            return "早餐或午餐安排茶点，晚餐避开热门餐厅排队高峰。";
        }
        if (dest.contains("上海")) {
            return "午餐选本帮菜或简餐，晚餐可结合外滩、陆家嘴或老城厢动线。";
        }
        if (dest.contains("北京")) {
            return "午餐以景区周边简餐为主，烤鸭等正餐建议提前预约。";
        }
        return "午餐放在当天动线中段，晚餐回到住宿片区附近，降低返程压力。";
    }

    private String buildBackupPlan(String dest, int day) {
        if (dest.contains("北京")) {
            return "若遇到排队或天气异常，可把户外点替换为国家博物馆、首都博物馆或商场休整。";
        }
        if (dest.contains("上海")) {
            return "若下雨或外滩人流过大，可改走上海博物馆、商场餐饮或法租界短线。";
        }
        if (dest.contains("成都")) {
            return "若熊猫基地或热门街区拥挤，可改为人民公园、四川博物院和茶馆慢行。";
        }
        if (dest.contains("三亚")) {
            return "若风浪或降雨影响海边活动，可改为免税店、酒店设施或室内餐饮休整。";
        }
        return day == 1 ? "若抵达延误，保留入住和晚餐，取消下午景点。"
                : "若天气或体力不佳，保留上午核心点，下午改为室内展馆、咖啡休息或住宿片区轻逛。";
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
