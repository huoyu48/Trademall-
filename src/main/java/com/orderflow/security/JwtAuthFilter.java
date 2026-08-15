package com.orderflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final List<String> permitAll;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider,
                        @Value("#{'${orderflow.security.permit-all:/api/auth/login,/api/platform/auth/login,/api/customer/auth/login,/actuator/**,/api/health}'.split(',')}")
                        List<String> permitAll) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.permitAll = permitAll;
    }

    private boolean isPermit(String uri) {
        return permitAll.stream().anyMatch(p -> antPathMatcher.match(p, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isPermit(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        try {
            if (header == null || !header.startsWith("Bearer ")) {
                writeUnauthorized(response, "缺少或无效的 Authorization 头");
                return;
            }
            String token = header.substring(7);
            if (!jwtTokenProvider.validate(token)) {
                writeUnauthorized(response, "登录已失效，请重新登录");
                return;
            }
            LoginUser user = jwtTokenProvider.parseToken(token);
            TenantContext.set(user.getTenantId(), user.getUserId(), user.getUsername());
            MDC.put("tenantId", String.valueOf(user.getTenantId()));
            var authorities = user.getRoles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
            var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new ObjectMapper()
                .writeValueAsString(ApiResponse.fail(40100, msg)));
    }
}
