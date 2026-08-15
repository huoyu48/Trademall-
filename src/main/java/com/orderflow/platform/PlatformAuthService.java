package com.orderflow.platform;

import com.orderflow.auth.TokenResponse;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.domain.entity.AppUser;
import com.orderflow.domain.entity.Role;
import com.orderflow.domain.entity.UserRole;
import com.orderflow.domain.mapper.AppUserMapper;
import com.orderflow.domain.mapper.RoleMapper;
import com.orderflow.domain.mapper.UserRoleMapper;
import com.orderflow.security.JwtTokenProvider;
import com.orderflow.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformAuthService {

    private final AppUserMapper appUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long expiresInMinutes;

    public PlatformAuthService(AppUserMapper appUserMapper, UserRoleMapper userRoleMapper,
                              RoleMapper roleMapper, PasswordEncoder passwordEncoder,
                              JwtTokenProvider jwtTokenProvider,
                              @Value("${orderflow.jwt.expiration-minutes:720}") long expiresInMinutes) {
        this.appUserMapper = appUserMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.expiresInMinutes = expiresInMinutes;
    }

    public TokenResponse login(String username, String password) {
        AppUser user = appUserMapper.findByUsername(username);
        if (user == null || user.getStatus() == 0
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(BizErrorCode.LOGIN_FAILED);
        }
        boolean isPlatform = userRoleMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRole>().eq("user_id", user.getId()))
                .stream()
                .map(ur -> roleMapper.selectById(ur.getRoleId()))
                .anyMatch(r -> r != null && "PLATFORM_ADMIN".equals(r.getRoleCode()));
        if (!isPlatform) {
            throw new BizException(BizErrorCode.LOGIN_FAILED);
        }

        LoginUser lu = LoginUser.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .roles(List.of("PLATFORM_ADMIN"))
                .build();
        TokenResponse resp = new TokenResponse();
        resp.setToken(jwtTokenProvider.createToken(lu));
        resp.setExpiresInMinutes(expiresInMinutes);
        resp.setUserId(user.getId());
        resp.setTenantId(user.getTenantId());
        resp.setUsername(user.getUsername());
        resp.setRoles(List.of("PLATFORM_ADMIN"));
        return resp;
    }
}
