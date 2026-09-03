package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.controller.AdminController;
import com.travelmate.entity.Destination;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.DestinationMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.service.NotificationCenterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminControllerWorkflowTests {

    private AdminController controller;
    private DestinationMapper destinationMapper;
    private TrafficOrderMapper trafficOrderMapper;
    private NotificationCenterService notificationCenterService;

    @BeforeEach
    void setUp() {
        controller = new AdminController();
        UserMapper userMapper = mock(UserMapper.class);
        destinationMapper = mock(DestinationMapper.class);
        trafficOrderMapper = mock(TrafficOrderMapper.class);
        notificationCenterService = mock(NotificationCenterService.class);

        User admin = new User();
        admin.setId(9L);
        admin.setUsername("admin-test");
        admin.setRole(1);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(admin);

        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "destinationMapper", destinationMapper);
        ReflectionTestUtils.setField(controller, "trafficOrderMapper", trafficOrderMapper);
        ReflectionTestUtils.setField(controller, "notificationCenterService", notificationCenterService);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-test", null, "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void syncHomeDestinationsInsertsMissingAndUpdatesExistingRows() {
        Destination beijing = destination("BEIJING", "北京");
        Destination shanghai = destination("shanghai", "上海");
        Destination existingShanghai = destination("shanghai", "旧上海");
        existingShanghai.setId(88L);
        existingShanghai.setCreateTime(LocalDateTime.of(2026, 8, 1, 9, 0));

        when(destinationMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null)
                .thenReturn(existingShanghai);
        when(destinationMapper.insert(any(Destination.class))).thenReturn(1);
        when(destinationMapper.updateById(any(Destination.class))).thenReturn(1);

        Result<Map<String, Object>> result = controller.syncHomeDestinations(List.of(beijing, shanghai));

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("total", 2)
                .containsEntry("inserted", 1)
                .containsEntry("updated", 1);
        assertThat(beijing.getSlug()).isEqualTo("beijing");
        assertThat(beijing.getSortOrder()).isEqualTo(10);
        assertThat(shanghai.getId()).isEqualTo(88L);
        assertThat(shanghai.getCreateTime()).isEqualTo(existingShanghai.getCreateTime());
        verify(destinationMapper).insert(beijing);
        verify(destinationMapper).updateById(shanghai);
    }

    @Test
    void completeTicketingMovesPaidTrafficOrderToTicketedAndNotifiesUser() {
        TrafficOrder order = new TrafficOrder();
        order.setOrderNo("TR202608250001");
        order.setUserId(42L);
        order.setOrderType(1);
        order.setStatus(1);
        when(trafficOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        when(trafficOrderMapper.markTicketed(order.getOrderNo())).thenReturn(1);

        Result<String> result = controller.completeTicketing(order.getOrderNo());

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("出票已完成");
        verify(trafficOrderMapper).markTicketed(order.getOrderNo());
        verify(notificationCenterService).createNotification(
                eq(42L), eq("traffic_order"), eq("火车票已出票"),
                eq("订单 TR202608250001 已完成出票，可在订单详情中查看。"),
                eq("/my-orders?tab=traffic"));
    }

    @Test
    void completeTicketingRejectsOrdersOutsideTicketingState() {
        TrafficOrder order = new TrafficOrder();
        order.setOrderNo("FL202608250001");
        order.setStatus(0);
        when(trafficOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order);

        Result<String> result = controller.completeTicketing(order.getOrderNo());

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).contains("只有出票中的交通订单");
        verify(trafficOrderMapper, never()).markTicketed(order.getOrderNo());
    }

    private Destination destination(String slug, String name) {
        Destination destination = new Destination();
        destination.setSlug(slug);
        destination.setName(name);
        destination.setCountry("中国");
        destination.setTag("城市标签");
        destination.setImg("/images/seed/city.svg");
        destination.setDesc("短描述");
        destination.setIntro("城市介绍");
        destination.setStatus(1);
        return destination;
    }
}
