package com.orderflow.audit;

public interface AuditLogService {
    void write(String action, String targetType, String targetId,
               String beforeSnapshot, String afterSnapshot);
}
