package com.orderflow.chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long id;
    private Long conversationId;
    private String senderType;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
