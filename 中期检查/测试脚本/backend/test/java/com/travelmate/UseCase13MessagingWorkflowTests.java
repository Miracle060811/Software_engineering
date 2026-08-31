package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.dto.PrivateMessageSendDTO;
import com.travelmate.entity.Notification;
import com.travelmate.entity.PrivateContact;
import com.travelmate.entity.PrivateMessage;
import com.travelmate.mapper.NotificationMapper;
import com.travelmate.mapper.PrivateContactMapper;
import com.travelmate.mapper.PrivateMessageMapper;
import com.travelmate.service.impl.NotificationCenterServiceImpl;
import com.travelmate.service.impl.PrivateMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UseCase13MessagingWorkflowTests {

    @Test
    void unitTc113CreatesUnreadNotificationAndIsolatesNotificationFailure() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        NotificationCenterServiceImpl service = new NotificationCenterServiceImpl();
        ReflectionTestUtils.setField(service, "notificationMapper", mapper);

        service.createNotification(9L, "comment", "收到回复", "有人回复了你", "/post/1");

        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getIsRead()).isZero();
        assertThat(captor.getValue().getActionUrl()).isEqualTo("/post/1");

        doThrow(new RuntimeException("notification db unavailable")).when(mapper).insert(any(Notification.class));
        assertThatCode(() -> service.createNotification(9L, "system", "标题", "内容"))
                .doesNotThrowAnyException();
    }

    @Test
    void intTc113SendsMessageAndCreatesBothContactSides() {
        PrivateMessageMapper messageMapper = mock(PrivateMessageMapper.class);
        PrivateContactMapper contactMapper = mock(PrivateContactMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PrivateMessageServiceImpl service = service(messageMapper, contactMapper, userMapper);
        User receiver = new User();
        receiver.setId(2L);
        receiver.setStatus(1);
        receiver.setDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(receiver);
        when(contactMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(messageMapper.insert(any(PrivateMessage.class))).thenAnswer(invocation -> {
            invocation.<PrivateMessage>getArgument(0).setId(55L);
            return 1;
        });

        PrivateMessageSendDTO dto = new PrivateMessageSendDTO();
        dto.setReceiverId(2L);
        dto.setContent("  明天一起出发吗？  ");
        PrivateMessage message = service.sendMessage(1L, dto);

        assertThat(message.getContent()).isEqualTo("明天一起出发吗？");
        assertThat(message.getReadStatus()).isZero();
        verify(contactMapper, times(2)).insert(any(PrivateContact.class));
    }

    @Test
    void unitTc113RejectsSelfAndBlankMessages() {
        PrivateMessageServiceImpl service = service(
                mock(PrivateMessageMapper.class), mock(PrivateContactMapper.class), mock(UserMapper.class));
        PrivateMessageSendDTO self = new PrivateMessageSendDTO();
        self.setReceiverId(1L);
        self.setContent("hello");
        assertThatThrownBy(() -> service.sendMessage(1L, self)).hasMessage("不能给自己发送私信");

        PrivateMessageSendDTO blank = new PrivateMessageSendDTO();
        blank.setReceiverId(2L);
        blank.setContent("  ");
        assertThatThrownBy(() -> service.sendMessage(1L, blank)).hasMessage("消息内容不能为空");
    }

    private PrivateMessageServiceImpl service(PrivateMessageMapper messageMapper,
                                              PrivateContactMapper contactMapper,
                                              UserMapper userMapper) {
        PrivateMessageServiceImpl service = new PrivateMessageServiceImpl();
        ReflectionTestUtils.setField(service, "privateMessageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "privateContactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        return service;
    }
}
