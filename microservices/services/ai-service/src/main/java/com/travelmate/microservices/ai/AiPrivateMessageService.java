package com.travelmate.microservices.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.dto.PrivateMessageSendDTO;
import com.travelmate.entity.PrivateContact;
import com.travelmate.entity.PrivateMessage;
import com.travelmate.mapper.PrivateContactMapper;
import com.travelmate.mapper.PrivateMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
public class AiPrivateMessageService {
    private final PrivateMessageMapper messageMapper;
    private final PrivateContactMapper contactMapper;
    private final AiIdentityGateway identityGateway;

    public AiPrivateMessageService(PrivateMessageMapper messageMapper, PrivateContactMapper contactMapper,
                                   AiIdentityGateway identityGateway) {
        this.messageMapper = messageMapper;
        this.contactMapper = contactMapper;
        this.identityGateway = identityGateway;
    }

    @Transactional
    public PrivateMessage send(Long userId, PrivateMessageSendDTO dto) {
        if (dto == null || dto.getReceiverId() == null) throw new RuntimeException("请选择收信人");
        if (Objects.equals(userId, dto.getReceiverId())) throw new RuntimeException("不能给自己发送私信");
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()) throw new RuntimeException("消息内容不能为空");
        if (content.length() > 1000) throw new RuntimeException("消息内容不能超过1000字");
        if (!identityGateway.isAvailable(dto.getReceiverId())) throw new RuntimeException("用户不存在或不可用");
        PrivateMessage message = new PrivateMessage();
        message.setSenderId(userId); message.setReceiverId(dto.getReceiverId()); message.setContent(content);
        message.setReadStatus(0); message.setDeleted(0); message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
        upsert(userId, dto.getReceiverId(), message.getId(), false);
        upsert(dto.getReceiverId(), userId, message.getId(), true);
        return message;
    }

    @Transactional
    public List<PrivateMessage> conversation(Long userId, Long contactUserId) {
        if (!identityGateway.isAvailable(contactUserId)) throw new RuntimeException("用户不存在或不可用");
        messageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getSenderId, contactUserId).eq(PrivateMessage::getReceiverId, userId)
                .eq(PrivateMessage::getReadStatus, 0).set(PrivateMessage::getReadStatus, 1));
        contactMapper.update(null, new LambdaUpdateWrapper<PrivateContact>()
                .eq(PrivateContact::getUserId, userId).eq(PrivateContact::getContactUserId, contactUserId)
                .set(PrivateContact::getUnreadCount, 0));
        return messageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getDeleted, 0)
                .and(q -> q.eq(PrivateMessage::getSenderId, userId).eq(PrivateMessage::getReceiverId, contactUserId)
                        .or().eq(PrivateMessage::getSenderId, contactUserId).eq(PrivateMessage::getReceiverId, userId))
                .orderByAsc(PrivateMessage::getCreateTime).orderByAsc(PrivateMessage::getId));
    }

    public int unreadCount(Long userId) {
        return contactMapper.selectList(new LambdaQueryWrapper<PrivateContact>().eq(PrivateContact::getUserId, userId))
                .stream().map(PrivateContact::getUnreadCount).filter(Objects::nonNull).reduce(0, Integer::sum);
    }

    public List<Map<String, Object>> contacts(Long userId) {
        List<PrivateContact> contacts = contactMapper.selectList(new LambdaQueryWrapper<PrivateContact>()
                .eq(PrivateContact::getUserId, userId).orderByDesc(PrivateContact::getUpdateTime));
        if (contacts.isEmpty()) return List.of();
        Set<Long> userIds = contacts.stream().map(PrivateContact::getContactUserId).collect(Collectors.toSet());
        Set<Long> messageIds = contacts.stream().map(PrivateContact::getLastMessageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AiIdentityGateway.UserSummary> users = identityGateway.findUsers(userIds);
        Map<Long, PrivateMessage> messages = messageIds.isEmpty() ? Map.of()
                : messageMapper.selectBatchIds(messageIds).stream()
                        .collect(Collectors.toMap(PrivateMessage::getId, Function.identity()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PrivateContact contact : contacts) {
            AiIdentityGateway.UserSummary user = users.get(contact.getContactUserId());
            if (user == null) continue;
            Map<String, Object> row = userSummary(user);
            PrivateMessage message = messages.get(contact.getLastMessageId());
            row.put("lastMessage", message == null ? "" : message.getContent());
            row.put("lastMessageTime", message == null ? contact.getUpdateTime() : message.getCreateTime());
            row.put("unreadCount", contact.getUnreadCount() == null ? 0 : contact.getUnreadCount());
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> searchUsers(Long userId, String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) return List.of();
        return identityGateway.searchUsers(userId, normalized).stream().map(this::userSummary).toList();
    }

    private Map<String, Object> userSummary(AiIdentityGateway.UserSummary user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.id());
        row.put("username", user.username());
        row.put("nickname", user.nickname());
        row.put("avatar", user.avatar());
        row.put("bio", user.bio());
        return row;
    }

    private void upsert(Long userId, Long contactUserId, Long messageId, boolean incrementUnread) {
        PrivateContact contact = contactMapper.selectOne(new LambdaQueryWrapper<PrivateContact>()
                .eq(PrivateContact::getUserId, userId).eq(PrivateContact::getContactUserId, contactUserId));
        if (contact == null) {
            contact = new PrivateContact(); contact.setUserId(userId); contact.setContactUserId(contactUserId);
            contact.setLastMessageId(messageId); contact.setUnreadCount(incrementUnread ? 1 : 0);
            contact.setCreateTime(LocalDateTime.now()); contact.setUpdateTime(LocalDateTime.now());
            contactMapper.insert(contact);
            return;
        }
        LambdaUpdateWrapper<PrivateContact> update = new LambdaUpdateWrapper<PrivateContact>()
                .eq(PrivateContact::getId, contact.getId()).set(PrivateContact::getLastMessageId, messageId)
                .set(PrivateContact::getUpdateTime, LocalDateTime.now());
        if (incrementUnread) update.setSql("unread_count = unread_count + 1");
        else update.set(PrivateContact::getUnreadCount, 0);
        contactMapper.update(null, update);
    }
}
