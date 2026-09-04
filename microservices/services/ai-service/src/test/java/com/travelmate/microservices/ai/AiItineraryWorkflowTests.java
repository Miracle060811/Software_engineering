package com.travelmate.microservices.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiPlan;
import com.travelmate.entity.Notification;
import com.travelmate.mapper.AiPlanMapper;
import com.travelmate.mapper.NotificationMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AiItineraryWorkflowTests {
    private final ObjectMapper json = new ObjectMapper();
    private final AiPlanMapper plans = mock(AiPlanMapper.class);
    private final NotificationMapper notifications = mock(NotificationMapper.class);
    private final AiItineraryService service = new AiItineraryService();
    private HttpServer server;
    private final AtomicReference<JsonNode> sent = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "aiPlanMapper", plans);
        ReflectionTestUtils.setField(service, "notificationMapper", notifications);
        ReflectionTestUtils.setField(service, "travelPlaceService", new TravelPlaceService());
        AiTravelContextGateway catalog = mock(AiTravelContextGateway.class);
        when(catalog.attractions(any())).thenReturn("西湖，地址：杭州湖滨");
        ReflectionTestUtils.setField(service, "travelContextGateway", catalog);
        doAnswer(call -> { call.<AiPlan>getArgument(0).setId(901L); return 1; })
                .when(plans).insert(any(AiPlan.class));
    }

    @AfterEach
    void stop() { if (server != null) server.stop(0); }

    private AiPlanCreateDTO request() {
        AiPlanCreateDTO dto = new AiPlanCreateDTO();
        dto.setOrigin("上海"); dto.setDestination("杭州"); dto.setDays(1);
        dto.setStartDate(LocalDate.now().plusDays(7).toString());
        dto.setPeopleCount(2); dto.setMustVisit("西湖");
        dto.setAvoidPlaces("高强度爬山"); dto.setTransportPreference("公共交通");
        return dto;
    }

    private void provider(int status, String content) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            sent.set(json.readTree(exchange.getRequestBody()));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = json.writeValueAsBytes(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
        ReflectionTestUtils.setField(service, "planModel", "test-model");
    }

    @Test
    void realHttpResponseIsValidatedSavedAndNotified() throws Exception {
        AiPlanCreateDTO dto = request();
        provider(200, new AiPlanValidationTests().validPlan().replace("2026-08-26", dto.getStartDate()));
        AiPlan plan = service.generatePlan(dto, 42L);
        JsonNode content = json.readTree(plan.getPlanContent());
        assertEquals("deepseek", content.path("generationSource").asText());
        assertEquals("test-model", sent.get().path("model").asText());
        assertEquals("Bearer test-key", auth.get());
        String prompt = sent.get().path("messages").path(1).path("content").asText();
        assertTrue(prompt.contains("西湖"));
        assertTrue(prompt.contains("高强度爬山"));
        assertTrue(prompt.contains("公共交通"));
        assertTrue(content.path("days").get(0).path("activities").get(0).isObject());
        assertEquals(42L, plan.getUserId());
        ArgumentCaptor<Notification> notice = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).insert(notice.capture());
        assertEquals("/ai-plan?planId=901", notice.getValue().getActionUrl());
        assertEquals(42L, notice.getValue().getUserId());
    }

    @Test
    void malformedProviderContentFallsBackToFullItinerary() throws Exception {
        provider(200, "{\"days\":[\"城市漫步\"]}");
        assertFallback();
    }

    @Test
    void providerFailureFallsBack() throws Exception {
        provider(503, "unavailable");
        assertFallback();
    }

    @Test
    void missingKeyFallsBackWithoutNetwork() throws Exception { assertFallback(); }

    private void assertFallback() throws Exception {
        AiPlanCreateDTO dto = request(); dto.setDays(2);
        JsonNode content = json.readTree(service.generatePlan(dto, 42L).getPlanContent());
        assertEquals("local-fallback", content.path("generationSource").asText());
        assertEquals(2, content.path("days").size());
        assertTrue(content.path("locationVerified").asBoolean());
        assertTrue(content.path("beforeTripChecklist").size() >= 4);
        for (JsonNode day : content.path("days")) {
            assertTrue(day.path("activities").size() >= 3);
            assertTrue(day.path("activities").get(0).isObject());
            assertFalse(day.path("backupPlan").asText().isBlank());
        }
    }

    @Test
    void unknownAndSameCitiesNeverPersist() {
        AiPlanCreateDTO dto = request(); dto.setDestination("酒馆蛋炒饭");
        assertThrows(TravelPlaceService.TravelPlaceException.class, () -> service.generatePlan(dto, 42L));
        dto.setDestination("上海市");
        assertThrows(TravelPlaceService.TravelPlaceException.class, () -> service.generatePlan(dto, 42L));
        verifyNoInteractions(plans, notifications);
    }

    @Test
    void catalogUsesHttpAndHandlesUnavailableService() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/attraction/search", exchange -> {
            byte[] response = "{\"code\":200,\"data\":[{\"name\":\"西湖\",\"address\":\"杭州\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        AiTravelContextGateway gateway = new AiTravelContextGateway("http://127.0.0.1:" + server.getAddress().getPort(), json);
        assertTrue(gateway.attractions("杭州市").contains("西湖"));
        server.stop(0);
        assertEquals(AiTravelContextGateway.UNAVAILABLE, gateway.attractions("杭州"));
    }
}
