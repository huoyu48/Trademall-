package com.orderflow.platform;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 平台治理：新增租户（让新商家进驻商城）。
 * 创建租户后自动为其生成一个商家管理员账号（默认 admin-{tenantCode}/admin123）。
 */
@Service
public class PlatformTenantService {

    private final TenantMapper tenantMapper;
    private final AppUserMapper appUserMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public PlatformTenantService(TenantMapper tenantMapper, AppUserMapper appUserMapper,
                                 RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                                 PasswordEncoder passwordEncoder) {
        this.tenantMapper = tenantMapper;
        this.appUserMapper = appUserMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Tenant createTenant(CreateTenantRequest req) {
        Tenant existing = tenantMapper.selectOne(
                new QueryWrapper<Tenant>().eq("tenant_code", req.getTenantCode()));
        if (existing != null) {
            throw new BizException(BizErrorCode.TENANT_CODE_DUPLICATED);
        }

        Tenant t = new Tenant();
        t.setTenantCode(req.getTenantCode());
        t.setTenantName(req.getTenantName());
        t.setStatus(1);
        tenantMapper.insert(t);

        String username = StringUtils.hasText(req.getAdminUsername()) ? req.getAdminUsername() : "admin-" + req.getTenantCode();
        String password = StringUtils.hasText(req.getAdminPassword()) ? req.getAdminPassword() : "admin123";
        createAdmin(username, password, t.getId());

        return t;
    }

    private void createAdmin(String username, String rawPassword, Long tenantId) {
        AppUser existing = appUserMapper.findByUsername(username);
        if (existing != null) {
            throw new BizException(40909, "管理员账号已存在：" + username);
        }
        AppUser u = new AppUser();
        u.setTenantId(tenantId);
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setStatus(1);
        appUserMapper.insert(u);

        Role role = roleMapper.selectOne(
                new QueryWrapper<Role>().eq("role_code", "MERCHANT_ADMIN"));
        UserRole ur = new UserRole();
        ur.setUserId(u.getId());
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);
    }
}
