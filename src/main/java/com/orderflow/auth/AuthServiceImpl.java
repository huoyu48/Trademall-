package com.orderflow.auth;

import com.orderflow.common.BizException;
import com.orderflow.common.BizErrorCode;
import com.orderflow.domain.entity.AppUser;
import com.orderflow.domain.entity.Role;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.entity.UserRole;
import com.orderflow.domain.mapper.AppUserMapper;
import com.orderflow.domain.mapper.RoleMapper;
import com.orderflow.domain.mapper.TenantMapper;
import com.orderflow.domain.mapper.UserRoleMapper;
import com.orderflow.security.JwtTokenProvider;
import com.orderflow.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserMapper appUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final TenantMapper tenantMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final long expiresInMinutes;

    public AuthServiceImpl(AppUserMapper appUserMapper,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper,
                           TenantMapper tenantMapper,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder,
                           @Value("${orderflow.jwt.expiration-minutes:720}") long expiresInMinutes) {
        this.appUserMapper = appUserMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.tenantMapper = tenantMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.expiresInMinutes = expiresInMinutes;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        AppUser user = appUserMapper.findByUsername(request.getUsername());
        if (user == null || user.getStatus() == 0
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(BizErrorCode.LOGIN_FAILED);
        }
        List<String> roles = loadRoles(user.getId());

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .roles(roles)
                .build();

        TokenResponse resp = new TokenResponse();
        resp.setToken(jwtTokenProvider.createToken(loginUser));
        resp.setExpiresInMinutes(expiresInMinutes);
        resp.setUserId(user.getId());
        resp.setTenantId(user.getTenantId());
        Tenant tenant = user.getTenantId() != null ? tenantMapper.selectById(user.getTenantId()) : null;
        resp.setTenantName(tenant != null ? tenant.getTenantName() : null);
        resp.setUsername(user.getUsername());
        resp.setRoles(roles);
        return resp;
    }

    private List<String> loadRoles(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRole>()
                        .eq("user_id", userId));
        return userRoles.stream()
                .map(ur -> roleMapper.selectById(ur.getRoleId()))
                .filter(r -> r != null)
                .map(Role::getRoleCode)
                .toList();
    }
}
