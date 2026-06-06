package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.dto.PrivateMessageSendDTO;
import com.travelmate.entity.PrivateContact;
import com.travelmate.entity.PrivateMessage;
import com.travelmate.mapper.PrivateContactMapper;
import com.travelmate.mapper.PrivateMessageMapper;
import com.travelmate.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {

    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Autowired
    private PrivateContactMapper privateContactMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Map<String, Object>> listContacts(Long currentUserId) {
        List<PrivateContact> contacts = privateContactMapper.selectList(new LambdaQueryWrapper<PrivateContact>()
                .eq(PrivateContact::getUserId, currentUserId)
                .orderByDesc(PrivateContact::getUpdateTime));
        if (contacts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = contacts.stream().map(PrivateContact::getContactUserId).collect(Collectors.toSet());
        Set<Long> messageIds = contacts.stream()
                .map(PrivateContact::getLastMessageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, PrivateMessage> messageMap = messageIds.isEmpty()
                ? Collections.emptyMap()
                : privateMessageMapper.selectBatchIds(messageIds).stream()
                        .collect(Collectors.toMap(PrivateMessage::getId, Function.identity()));

        return contacts.stream()
                .map(contact -> buildContact(contact, userMap.get(contact.getContactUserId()), messageMap.get(contact.getLastMessageId())))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Map<String, Object>> searchUsers(Long currentUserId, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ne(User::getId, currentUserId)
                .eq(User::getStatus, 1)
                .and(wrapper -> wrapper.eq(User::getDeleted, 0).or().isNull(User::getDeleted))
                .and(wrapper -> wrapper.like(User::getUsername, kw).or().like(User::getNickname, kw))
                .last("LIMIT 20"));
        return users.stream().map(this::buildUserSummary).toList();
    }

    @Override
    @Transactional
    public List<PrivateMessage> listConversation(Long currentUserId, Long contactUserId) {
        assertTargetUser(contactUserId);
        privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getSenderId, contactUserId)
                .eq(PrivateMessage::getReceiverId, currentUserId)
                .eq(PrivateMessage::getReadStatus, 0)
                .set(PrivateMessage::getReadStatus, 1));
        privateContactMapper.update(null, new LambdaUpdateWrapper<PrivateContact>()
                .eq(PrivateContact::getUserId, currentUserId)
                .eq(PrivateContact::getContactUserId, contactUserId)
                .set(PrivateContact::getUnreadCount, 0));

        return privateMessageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getDeleted, 0)
                .and(wrapper -> wrapper
                        .eq(PrivateMessage::getSenderId, currentUserId)
                        .eq(PrivateMessage::getReceiverId, contactUserId)
                        .or()
                        .eq(PrivateMessage::getSenderId, contactUserId)
                        .eq(PrivateMessage::getReceiverId, currentUserId))
                .orderByAsc(PrivateMessage::getCreateTime)
                .orderByAsc(PrivateMessage::getId));
    }

    @Override
    @Transactional
    public PrivateMessage sendMessage(Long currentUserId, PrivateMessageSendDTO dto) {
        if (dto == null || dto.getReceiverId() == null) {
            throw new RuntimeException("请选择收信人");
        }
        if (Objects.equals(currentUserId, dto.getReceiverId())) {
            throw new RuntimeException("不能给自己发送私信");
        }
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }
        if (content.length() > 1000) {
            throw new RuntimeException("消息内容不能超过1000字");
        }
        assertTargetUser(dto.getReceiverId());

        PrivateMessage message = new PrivateMessage();
        message.setSenderId(currentUserId);
        message.setReceiverId(dto.getReceiverId());
        message.setContent(content);
        message.setReadStatus(0);
        message.setDeleted(0);
        message.setCreateTime(LocalDateTime.now());
        privateMessageMapper.insert(message);

        upsertContact(currentUserId, dto.getReceiverId(), message.getId(), false);
        upsertContact(dto.getReceiverId(), currentUserId, message.getId(), true);
        return message;
    }

    @Override
    public Integer unreadCount(Long currentUserId) {
        List<PrivateContact> contacts = privateContactMapper.selectList(new LambdaQueryWrapper<PrivateContact>()
                .eq(PrivateContact::getUserId, currentUserId));
        return contacts.stream()
                .map(PrivateContact::getUnreadCount)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private void upsertContact(Long userId, Long contactUserId, Long messageId, boolean incrementUnread) {
        PrivateContact existing = privateContactMapper.selectOne(new LambdaQueryWrapper<PrivateContact>()
                .eq(PrivateContact::getUserId, userId)
                .eq(PrivateContact::getContactUserId, contactUserId));
        if (existing == null) {
            PrivateContact contact = new PrivateContact();
            contact.setUserId(userId);
            contact.setContactUserId(contactUserId);
            contact.setLastMessageId(messageId);
            contact.setUnreadCount(incrementUnread ? 1 : 0);
            privateContactMapper.insert(contact);
            return;
        }

        LambdaUpdateWrapper<PrivateContact> update = new LambdaUpdateWrapper<PrivateContact>()
                .eq(PrivateContact::getId, existing.getId())
                .set(PrivateContact::getLastMessageId, messageId)
                .set(PrivateContact::getUpdateTime, LocalDateTime.now());
        if (incrementUnread) {
            update.setSql("unread_count = unread_count + 1");
        } else {
            update.set(PrivateContact::getUnreadCount, 0);
        }
        privateContactMapper.update(null, update);
    }

    private User assertTargetUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getStatus(), 0) || Objects.equals(user.getDeleted(), 1)) {
            throw new RuntimeException("用户不存在或不可用");
        }
        return user;
    }

    private Map<String, Object> buildContact(PrivateContact contact, User user, PrivateMessage lastMessage) {
        if (user == null) {
            return null;
        }
        Map<String, Object> data = buildUserSummary(user);
        data.put("lastMessage", lastMessage == null ? "" : lastMessage.getContent());
        data.put("lastMessageTime", lastMessage == null ? contact.getUpdateTime() : lastMessage.getCreateTime());
        data.put("unreadCount", contact.getUnreadCount() == null ? 0 : contact.getUnreadCount());
        return data;
    }

    private Map<String, Object> buildUserSummary(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("bio", user.getBio());
        return data;
    }
}
