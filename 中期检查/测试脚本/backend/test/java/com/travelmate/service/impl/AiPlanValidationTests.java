package com.travelmate.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travelmate.dto.AiPlanCreateDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiPlanValidationTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void acceptsCompletePlanAndPinsVerifiedRoute() throws Exception {
        AiServiceImpl service = new AiServiceImpl();
        AiPlanCreateDTO dto = request();
        String normalized = service.normalizePlanContent(
                validPlan().replace("无需预付", "现场购票，门票45元/人"), dto);

        assertNotNull(normalized);
        JsonNode root = MAPPER.readTree(normalized);
        assertEquals("上海", root.path("origin").asText());
        assertEquals("杭州", root.path("destination").asText());
        assertTrue(root.path("locationVerified").asBoolean());
        assertEquals("出发前通过景区官方渠道核对预约、票价和开放信息",
                root.path("days").path(0).path("activities").path(1).path("bookingTip").asText());
    }

    @Test
    void rejectsWrongDateOrderAndUnsupportedRealtimeGuarantee() {
        AiServiceImpl service = new AiServiceImpl();
        AiPlanCreateDTO dto = request();

        assertNull(service.normalizePlanContent(validPlan().replace("2026-08-26", "2026-08-27"), dto));
        assertNull(service.normalizePlanContent(validPlan().replace("11:30", "08:30"), dto));
        assertNull(service.normalizePlanContent(validPlan().replace("节奏舒适", "保证有票"), dto));
    }

    @Test
    void compactsOverloadedAiDayAndRecalculatesCosts() throws Exception {
        AiServiceImpl service = new AiServiceImpl();
        ObjectNode root = (ObjectNode) MAPPER.readTree(validPlan());
        ObjectNode day = (ObjectNode) root.path("days").path(0);
        ObjectNode template = (ObjectNode) day.path("activities").path(0);
        ArrayNode activities = MAPPER.createArrayNode();
        for (int i = 0; i < 8; i++) {
            ObjectNode activity = template.deepCopy();
            activity.put("time", String.format("%02d:00", 8 + i));
            activity.put("cost", 10);
            activities.add(activity);
        }
        day.set("activities", activities);

        JsonNode normalized = MAPPER.readTree(service.normalizePlanContent(root.toString(), request()));
        assertEquals(5, normalized.path("days").path(0).path("activities").size());
        assertEquals(50, normalized.path("days").path(0).path("dayEstimatedCost").asInt());
        assertEquals(50, normalized.path("totalEstimatedCost").asInt());
    }

    private AiPlanCreateDTO request() {
        AiPlanCreateDTO dto = new AiPlanCreateDTO();
        dto.setOrigin("上海");
        dto.setDestination("杭州");
        dto.setDays(1);
        dto.setStartDate("2026-08-26");
        return dto;
    }

    private String validPlan() {
        return """
                {"title":"杭州1日行程","summary":"节奏舒适","pace":"适中",
                 "budgetNote":"不含往返交通与住宿","transportAdvice":"查询实际班次并预留缓冲",
                 "hotelAdvice":"选择交通便利且夜间返回方便的区域",
                 "beforeTripChecklist":["带证件","查预约","核交通","备雨具"],
                 "riskNotes":["注意天气","注意排队","注意体力"],
                 "alternatives":[
                   {"title":"雨天","whenToUse":"下雨时","changes":"改室内活动"},
                   {"title":"低体力","whenToUse":"疲劳时","changes":"减少步行"}],
                 "days":[{"day":1,"date":"2026-08-26","theme":"城市慢行","area":"湖滨片区",
                   "dayEstimatedCost":300,"tips":"预留返程时间","mealHint":"同区正规餐馆用餐",
                   "backupPlan":"下雨时改室内展馆","activities":[
                     {"time":"09:30","name":"城市慢行","description":"按同区顺路游览并核对开放信息",
                      "type":"景点","duration":"约2小时","transfer":"步行约15分钟","bookingTip":"确认开放信息","cost":100},
                     {"time":"11:30","name":"午餐休息","description":"选择明码标价且近期评价稳定的正规餐馆",
                      "type":"餐饮","duration":"约1小时","transfer":"同区步行","bookingTip":"无需预付","cost":100}] }],
                 "totalEstimatedCost":300}
                """;
    }
}
