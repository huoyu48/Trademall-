package com.orderflow.auth;

import com.orderflow.domain.entity.AppUser;
import com.orderflow.domain.entity.Role;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.entity.UserRole;
import com.orderflow.domain.mapper.AppUserMapper;
import com.orderflow.domain.mapper.RoleMapper;
import com.orderflow.domain.mapper.TenantMapper;
import com.orderflow.domain.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 演示用种子数据：两个租户 A / B，各自一个管理员账号（密码均为 admin123）。
 * 仅用于本地演示，生产环境应使用真实初始化脚本。
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TenantMapper tenantMapper;
    private final RoleMapper roleMapper;
    private final AppUserMapper appUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(TenantMapper tenantMapper, RoleMapper roleMapper,
                           AppUserMapper appUserMapper, UserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder) {
        this.tenantMapper = tenantMapper;
        this.roleMapper = roleMapper;
        this.appUserMapper = appUserMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureRole("PLATFORM_ADMIN", "平台管理员");
        ensureRole("MERCHANT_ADMIN", "商家管理员");
        ensureRole("CUSTOMER", "顾客");
        Long tenantA = ensureTenant("t-a", "演示租户A");
        Long tenantB = ensureTenant("t-b", "演示租户B");
        ensureUser("admin-a", "admin123", tenantA);
        ensureUser("admin-b", "admin123", tenantB);
        // 平台管理员不归属任何商家租户，tenantId 用 0 作为哨兵值，由跨租户开关绕过隔离
        ensurePlatformUser("platform-admin", "admin123");
        log.info("种子数据初始化完成：租户A(t-a)/admin-a，租户B(t-b)/admin-b，平台管理员 platform-admin，密码均为 admin123");
    }

    private void ensureRole(String code, String name) {
        Role existing = roleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>().eq("role_code", code));
        if (existing == null) {
            Role r = new Role();
            r.setRoleCode(code);
            r.setRoleName(name);
            roleMapper.insert(r);
        }
    }

    private Long ensureTenant(String code, String name) {
        Tenant existing = tenantMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Tenant>().eq("tenant_code", code));
        if (existing != null) {
            return existing.getId();
        }
        Tenant t = new Tenant();
        t.setTenantCode(code);
        t.setTenantName(name);
        t.setStatus(1);
        tenantMapper.insert(t);
        return t.getId();
    }

    private void ensureUser(String username, String rawPassword, Long tenantId) {
        AppUser existing = appUserMapper.findByUsername(username);
        if (existing != null) {
            return;
        }
        AppUser u = new AppUser();
        u.setTenantId(tenantId);
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setStatus(1);
        appUserMapper.insert(u);

        Role role = roleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>().eq("role_code", "MERCHANT_ADMIN"));
        UserRole ur = new UserRole();
        ur.setUserId(u.getId());
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);
    }

    private void ensurePlatformUser(String username, String rawPassword) {
        AppUser existing = appUserMapper.findByUsername(username);
        if (existing != null) {
            return;
        }
        AppUser u = new AppUser();
        u.setTenantId(0L); // 哨兵值：平台管理员跨租户
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setStatus(1);
        appUserMapper.insert(u);

        Role role = roleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>().eq("role_code", "PLATFORM_ADMIN"));
        UserRole ur = new UserRole();
        ur.setUserId(u.getId());
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);
    }
}
