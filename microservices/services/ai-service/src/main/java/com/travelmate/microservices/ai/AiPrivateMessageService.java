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
