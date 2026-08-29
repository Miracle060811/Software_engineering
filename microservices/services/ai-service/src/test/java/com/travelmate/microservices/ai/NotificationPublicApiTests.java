package com.travelmate.microservices.ai;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travelmate.common.UserContext;
import com.travelmate.entity.Notification;
import com.travelmate.mapper.NotificationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationPublicApiTests {
    private NotificationMapper notificationMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "notification-api-test"),
                Notification.class);
        notificationMapper = mock(NotificationMapper.class);
        UserContext userContext = mock(UserContext.class);
        when(userContext.getCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationQueryController(notificationMapper, userContext))
                .build();
    }

    @Test
    void notificationQueriesReturnCurrentUsersData() throws Exception {
        Notification notification = new Notification();
        notification.setId(31L);
        notification.setUserId(7L);
        when(notificationMapper.selectList(any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);

        mockMvc.perform(get("/api/notification/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(31));
        mockMvc.perform(get("/api/notification/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void notificationMutationsAreExposedAsApiOperations() throws Exception {
        mockMvc.perform(post("/api/notification/read/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/api/notification/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/api/notification/clear-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(notificationMapper).update(any(), any());
        verify(notificationMapper, org.mockito.Mockito.times(2)).delete(any());
    }
}
