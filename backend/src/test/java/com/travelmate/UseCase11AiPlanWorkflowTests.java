package com.travelmate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiPlan;
import com.travelmate.mapper.AiPlanMapper;
import com.travelmate.service.NotificationCenterService;
import com.travelmate.service.impl.AiServiceImpl;
import com.travelmate.service.impl.TravelPlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase11AiPlanWorkflowTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiServiceImpl service;
    private AiPlanMapper planMapper;
    private NotificationCenterService notificationCenter;

    @BeforeEach
    void setUp() {
        service = new AiServiceImpl();
        planMapper = mock(AiPlanMapper.class);
        notificationCenter = mock(NotificationCenterService.class);
        TravelPlaceService placeService = mock(TravelPlaceService.class);

        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "aiPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "notificationCenterService", notificationCenter);
        ReflectionTestUtils.setField(service, "travelPlaceService", placeService);

        when(placeService.verifyCity("上海", "出发地"))
                .thenReturn(new TravelPlaceService.VerifiedPlace(
                        "上海", "上海", "上海市", "CN", 31.23, 121.47, "local"));
        when(placeService.verifyCity("杭州", "目的地"))
                .thenReturn(new TravelPlaceService.VerifiedPlace(
                        "杭州", "杭州", "杭州市", "CN", 30.27, 120.15, "local"));
        doAnswer(invocation -> {
            AiPlan plan = invocation.getArgument(0);
            plan.setId(901L);
            return 1;
        }).when(planMapper).insert(any(AiPlan.class));
    }

    @Test
    void intTc111GeneratesPersistsAndNotifiesWithDeterministicFallback() throws Exception {
        AiPlan plan = service.generatePlan(request(), 42L);

        assertThat(plan.getId()).isEqualTo(901L);
        assertThat(plan.getUserId()).isEqualTo(42L);
        assertThat(plan.getDestination()).isEqualTo("杭州");
        assertThat(plan.getDays()).isEqualTo(2);
        assertThat(plan.getStartDate()).isEqualTo(LocalDate.parse("2026-09-01"));
        assertThat(plan.getStatus()).isEqualTo(1);

        JsonNode content = MAPPER.readTree(plan.getPlanContent());
        assertThat(content.path("origin").asText()).isEqualTo("上海");
        assertThat(content.path("destination").asText()).isEqualTo("杭州");
        assertThat(content.path("locationVerified").asBoolean()).isTrue();
        assertThat(content.path("days")).hasSize(2);
        assertThat(content.path("beforeTripChecklist").size()).isGreaterThanOrEqualTo(4);
        assertThat(content.path("riskNotes").size()).isGreaterThanOrEqualTo(3);

        ArgumentCaptor<AiPlan> persisted = ArgumentCaptor.forClass(AiPlan.class);
        verify(planMapper).insert(persisted.capture());
        assertThat(persisted.getValue()).isSameAs(plan);
        verify(notificationCenter).createNotification(
                eq(42L), eq("ai_plan"), eq("AI 行程已生成"), any(String.class), eq("/ai-plan?planId=901"));
    }

    @Test
    void intTc111ListsOnlyCurrentUsersPlans() {
        AiPlan plan = new AiPlan();
        plan.setId(7L);
        plan.setUserId(42L);
        when(planMapper.selectList(any())).thenReturn(List.of(plan));

        assertThat(service.listMyPlans(42L)).containsExactly(plan);
        verify(planMapper).selectList(any());
    }

    @Test
    void intTc111RejectsPlanOwnedByAnotherUser() {
        AiPlan plan = new AiPlan();
        plan.setId(7L);
        plan.setUserId(99L);
        when(planMapper.selectById(7L)).thenReturn(plan);

        assertThatThrownBy(() -> service.getPlanById(7L, 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void unitTc111RejectsMissingDestinationBeforePersistence() {
        AiPlanCreateDTO dto = request();
        dto.setDestination("   ");

        assertThatThrownBy(() -> service.generatePlan(dto, 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("目的地");
    }

    private AiPlanCreateDTO request() {
        AiPlanCreateDTO dto = new AiPlanCreateDTO();
        dto.setOrigin("上海");
        dto.setDestination("杭州");
        dto.setDays(2);
        dto.setBudget(new BigDecimal("3000"));
        dto.setPeopleCount(2);
        dto.setPreferences("美食,轻松");
        dto.setStartDate("2026-09-01");
        dto.setTravelStyle("轻松");
        dto.setTransportPreference("公共交通");
        dto.setAccommodationPreference("安静");
        return dto;
    }
}
