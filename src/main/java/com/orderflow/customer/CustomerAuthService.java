package com.orderflow.customer;

import com.orderflow.auth.TokenResponse;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.domain.entity.Customer;
import com.orderflow.domain.mapper.CustomerMapper;
import com.orderflow.security.JwtTokenProvider;
import com.orderflow.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerAuthService {

    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long expiresInMinutes;

    public CustomerAuthService(CustomerMapper customerMapper, PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider,
                               @Value("${orderflow.jwt.expiration-minutes:720}") long expiresInMinutes) {
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.expiresInMinutes = expiresInMinutes;
    }

    public TokenResponse login(String username, String password) {
        Customer c = customerMapper.findByUsername(username);
        if (c == null || c.getStatus() == null || c.getStatus() == 0
                || !passwordEncoder.matches(password, c.getPasswordHash())) {
            throw new BizException(BizErrorCode.LOGIN_FAILED);
        }
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
