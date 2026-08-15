package com.orderflow.platform;

import com.orderflow.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 平台管理员请求拦截器：对 /api/platform/** 开启"忽略租户隔离"开关，
 * 使平台接口能跨所有租户聚合查询。开关在请求结束时由 JwtAuthFilter 统一清理。
 */
@Component
public class PlatformTenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantContext.setIgnoreTenant(true);
        return true;
    }
}
