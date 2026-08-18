package com.orderflow.chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatConversationDTO {
    private Long id;
    private Long tenantId;
    private String peerName;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
}
