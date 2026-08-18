package com.orderflow.chat;

import com.orderflow.domain.entity.ChatConversation;
import com.orderflow.domain.mapper.AppUserMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** 向已连接的顾客和商家管理员推送新消息；客户端以 message.id 去重。 */
@Component
public class ChatRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final AppUserMapper appUserMapper;

    public ChatRealtimePublisher(SimpMessagingTemplate messagingTemplate, AppUserMapper appUserMapper) {
        this.messagingTemplate = messagingTemplate;
        this.appUserMapper = appUserMapper;
    }

    public void publish(ChatConversation conversation, ChatMessageDTO message) {
        messagingTemplate.convertAndSendToUser("CUSTOMER:" + conversation.getCustomerId(), "/queue/chat", message);
        for (Long merchantUserId : appUserMapper.findMerchantAdminIdsByTenant(conversation.getTenantId())) {
            messagingTemplate.convertAndSendToUser("MERCHANT:" + merchantUserId, "/queue/chat", message);
        }
    }
}
