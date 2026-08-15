package com.orderflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long actorId;
    private String action;
    private String targetType;
    private String targetId;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String requestId;
    private LocalDateTime createdAt;
}
