package com.orderflow.customer;

import com.orderflow.auth.TokenResponse;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.domain.entity.Customer;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.mapper.CustomerMapper;
import com.orderflow.domain.mapper.TenantMapper;
import com.orderflow.security.JwtTokenProvider;
import com.orderflow.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerAuthService {

    private final CustomerMapper customerMapper;
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long expiresInMinutes;

    private final String defaultTenantCode;

    public CustomerAuthService(CustomerMapper customerMapper, TenantMapper tenantMapper,
                               PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider,
                               @Value("${orderflow.jwt.expiration-minutes:720}") long expiresInMinutes,
                               @Value("${orderflow.customer.default-tenant-code:t-a}") String defaultTenantCode) {
        this.customerMapper = customerMapper;
        this.tenantMapper = tenantMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.expiresInMinutes = expiresInMinutes;
        this.defaultTenantCode = defaultTenantCode;
    }

    public TokenResponse login(String username, String password) {
        Customer c = customerMapper.findByUsername(username);
        if (c == null || c.getStatus() == null || c.getStatus() == 0
                || !passwordEncoder.matches(password, c.getPasswordHash())) {
            throw new BizException(BizErrorCode.LOGIN_FAILED);
        }
        return tokenResponse(c);
    }

    /**
     * 在默认商城租户下创建顾客账号。当前数据模型的 customer 归属单个租户，
     * 因此默认租户由配置指定，而非相信前端传入的 tenantId。
     */
    public TokenResponse register(CustomerRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(BizErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        if (customerMapper.findByUsername(request.getUsername()) != null) {
            throw new BizException(BizErrorCode.CUSTOMER_USERNAME_DUPLICATED);
        }

        Tenant tenant = tenantMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Tenant>()
                .eq("tenant_code", defaultTenantCode)
                .eq("status", 1));
        if (tenant == null) {
            throw new BizException(BizErrorCode.TENANT_NOT_FOUND);
        }

        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setUsername(request.getUsername());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? request.getUsername() : request.getNickname());
        customer.setPhone(request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone());
        customer.setStatus(1);
        try {
            customerMapper.insert(customer);
        } catch (DuplicateKeyException ex) {
            // 数据库唯一索引用于兜住“两个请求同时通过存在性检查”的竞态。
            throw new BizException(BizErrorCode.CUSTOMER_USERNAME_DUPLICATED);
        }
        return tokenResponse(customer);
    }

    private TokenResponse tokenResponse(Customer c) {
        LoginUser lu = LoginUser.builder()
                .userId(c.getId())
                .tenantId(c.getTenantId())
                .username(c.getUsername())
                .roles(List.of("CUSTOMER"))
                .build();
        TokenResponse resp = new TokenResponse();
        resp.setToken(jwtTokenProvider.createToken(lu));
        resp.setExpiresInMinutes(expiresInMinutes);
        resp.setUserId(c.getId());
        resp.setTenantId(c.getTenantId());
        resp.setUsername(c.getUsername());
        resp.setRoles(List.of("CUSTOMER"));
        return resp;
    }
}
