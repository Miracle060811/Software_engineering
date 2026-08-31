package com.travelmate;

import com.travelmate.entity.Attraction;
import com.travelmate.entity.AttractionOrder;
import com.travelmate.integration.local.LocalNotificationGateway;
import com.travelmate.mapper.AttractionMapper;
import com.travelmate.mapper.AttractionOrderMapper;
import com.travelmate.service.NotificationCenterService;
import com.travelmate.service.impl.AttractionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase07AttractionTicketWorkflowTests {

    private AttractionServiceImpl service;
    private AttractionMapper attractionMapper;
    private AttractionOrderMapper orderMapper;
    private NotificationCenterService notificationService;

    @BeforeEach
    void setUp() {
        service = new AttractionServiceImpl();
        attractionMapper = mock(AttractionMapper.class);
        orderMapper = mock(AttractionOrderMapper.class);
        notificationService = mock(NotificationCenterService.class);
        ReflectionTestUtils.setField(service, "baseMapper", attractionMapper);
        ReflectionTestUtils.setField(service, "attractionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "notificationGateway", new LocalNotificationGateway(notificationService));
    }

    @Test
    void intTc107CreatesTicketOrderWithAdultAndChildPricing() {
        when(attractionMapper.selectById(11L)).thenReturn(attraction(1));
        when(attractionMapper.deductTicket(11L, 3)).thenReturn(1);
        when(orderMapper.insert(any(AttractionOrder.class))).thenReturn(1);

        String orderNo = service.buyTicket(7L, 11L, 2, 1, "测试游客", "13800138000");

        assertThat(orderNo).startsWith("AT");
        ArgumentCaptor<AttractionOrder> captor = ArgumentCaptor.forClass(AttractionOrder.class);
        verify(orderMapper).insert(captor.capture());
        assertThat(captor.getValue().getTicketCount()).isEqualTo(3);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("250.00");
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void unitTc107RejectsNegativeAndExcessiveCountsBeforeInventoryMutation() {
        assertThatThrownBy(() -> service.buyTicket(7L, 11L, -1, 2, "游客", "13800138000"))
                .hasMessage("票数不能为负数");
        assertThatThrownBy(() -> service.buyTicket(7L, 11L, 10, 1, "游客", "13800138000"))
                .hasMessage("购买数量不合法");
        verify(attractionMapper, never()).deductTicket(any(), any());
    }

    @Test
    void unitTc107RejectsOfflineAttractionWithoutCreatingOrder() {
        when(attractionMapper.selectById(11L)).thenReturn(attraction(0));

        assertThatThrownBy(() -> service.buyTicket(7L, 11L, 1, 0, "游客", "13800138000"))
                .hasMessage("景点不存在或已下线");
        verify(orderMapper, never()).insert(any(AttractionOrder.class));
    }

    @Test
    void unitTc107RejectsInventoryRaceWithoutCreatingOrderOrNotification() {
        when(attractionMapper.selectById(11L)).thenReturn(attraction(1));
        when(attractionMapper.deductTicket(11L, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.buyTicket(7L, 11L, 1, 0, "游客", "13800138000"))
                .hasMessage("门票库存不足，购票失败");
        verify(orderMapper, never()).insert(any(AttractionOrder.class));
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    private Attraction attraction(int status) {
        Attraction attraction = new Attraction();
        attraction.setId(11L);
        attraction.setName("测试景点");
        attraction.setCity("北京");
        attraction.setStatus(status);
        attraction.setAdultPrice(new BigDecimal("100.00"));
        attraction.setChildPrice(new BigDecimal("50.00"));
        return attraction;
    }
}
