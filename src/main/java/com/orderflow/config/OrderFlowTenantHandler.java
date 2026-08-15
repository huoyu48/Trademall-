package com.orderflow.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.orderflow.security.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 多租户 SQL 拦截：对所有业务表自动追加 `tenant_id = ?` 条件。
 * 被忽略的表（租户/角色/用户）不加条件，避免登录时因未知 tenantId 而失败。
 *
 * 这是“机制级”隔离，而非仅靠业务代码自觉写 where tenant_id。
 */
@Component
public class OrderFlowTenantHandler implements TenantLineHandler {

    private static final Set<String> IGNORE_TABLES = Set.of(
            "tenant", "role", "user_role", "app_user", "order_item", "customer");

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("当前线程未设置 tenantId，无法执行租户隔离查询");
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 平台管理员跨租户查询时，对所有业务表跳过租户隔离
        return IGNORE_TABLES.contains(tableName) || TenantContext.isIgnoreTenant();
    }
}
