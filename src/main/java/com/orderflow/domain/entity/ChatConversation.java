package com.orderflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class ChatConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long customerId;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private Integer customerUnreadCount;
    private Integer merchantUnreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
