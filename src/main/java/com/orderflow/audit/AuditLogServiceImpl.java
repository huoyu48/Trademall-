package com.orderflow.audit;

import com.orderflow.domain.entity.AuditLog;
import com.orderflow.domain.mapper.AuditLogMapper;
import com.orderflow.security.TenantContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void write(String action, String targetType, String targetId,
                     String beforeSnapshot, String afterSnapshot) {
        AuditLog log = new AuditLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setActorId(TenantContext.getUserId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setBeforeSnapshot(beforeSnapshot);
        log.setAfterSnapshot(afterSnapshot);
        log.setRequestId(MDC.get("requestId"));
        auditLogMapper.insert(log);
    }
}
