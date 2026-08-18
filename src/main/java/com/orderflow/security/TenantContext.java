package com.orderflow.security;

/**
 * 租户上下文：基于 ThreadLocal 保存当前请求的身份与租户信息。
 * 注意：异步线程（@Async / MQ 消费 / AFTER_COMMIT 监听器）不会自动继承，
 * 跨线程时必须显式传递 tenantId（例如 Outbox 事件载荷）。
 */
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE_TENANT = ThreadLocal.withInitial(() -> false);

    private TenantContext() {
    }

    public static void set(Long tenantId, Long userId, String username) {
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    /**
     * 跨租户开关。平台管理员需要无视租户隔离查看全量数据时使用。
     * 仅由 PlatformTenantInterceptor 在 /api/platform/** 请求中开启。
     */
    public static void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT.set(ignore);
    }

    public static boolean isIgnoreTenant() {
        return Boolean.TRUE.equals(IGNORE_TENANT.get());
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        USERNAME.remove();
        IGNORE_TENANT.remove();
    }
}
