package com.orderflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 取出当前登录用户（JWT 过滤器已将 LoginUser 放入 principal）。
 * 商家 / 平台管理员 / 顾客三类身份共用，便于在 Controller 内取到 userId / tenantId / roles。
 */
public class SecurityUtils {

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu) {
            return lu;
        }
        return null;
    }
}
