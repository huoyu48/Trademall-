package com.orderflow.customer;

import com.orderflow.auth.TokenResponse;
import com.orderflow.common.BizException;
import com.orderflow.domain.entity.Customer;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.mapper.CustomerMapper;
import com.orderflow.domain.mapper.TenantMapper;
import com.orderflow.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {
    @Mock private CustomerMapper customerMapper;
    @Mock private TenantMapper tenantMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    private CustomerAuthService service;

    @BeforeEach
    void setUp() {
        service = new CustomerAuthService(customerMapper, tenantMapper, passwordEncoder,
                jwtTokenProvider, 720, "t-a");
    }

    @Test
    void registersIntoConfiguredTenantAndReturnsToken() {
        Tenant tenant = new Tenant();
        tenant.setId(11L);
        when(tenantMapper.selectOne(any())).thenReturn(tenant);
        when(passwordEncoder.encode("secret12")).thenReturn("encoded");
        when(jwtTokenProvider.createToken(any())).thenReturn("jwt-token");
        when(customerMapper.insert(any(Customer.class))).thenAnswer(invocation -> {
            ((Customer) invocation.getArgument(0)).setId(101L);
            return 1;
        });

        CustomerRegisterRequest request = request("new_customer", "secret12", "secret12");
        TokenResponse response = service.register(request);

        ArgumentCaptor<Customer> customer = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(customer.capture());
        assertEquals(11L, customer.getValue().getTenantId());
        assertEquals("encoded", customer.getValue().getPasswordHash());
        assertEquals("jwt-token", response.getToken());
        assertEquals(101L, response.getUserId());
    }

    @Test
    void rejectsDuplicateUsernameIncludingConcurrentInsert() {
        when(customerMapper.findByUsername("taken")).thenReturn(new Customer());
        assertThrows(BizException.class, () -> service.register(request("taken", "secret12", "secret12")));

        when(customerMapper.findByUsername("race")).thenReturn(null);
        Tenant tenant = new Tenant();
        tenant.setId(11L);
        when(tenantMapper.selectOne(any())).thenReturn(tenant);
        when(customerMapper.insert(any(Customer.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThrows(BizException.class, () -> service.register(request("race", "secret12", "secret12")));
    }

    private CustomerRegisterRequest request(String username, String password, String confirmPassword) {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
