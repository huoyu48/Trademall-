package com.orderflow.customer;

import com.orderflow.auth.LoginRequest;
import com.orderflow.auth.TokenResponse;
import com.orderflow.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/auth")
public class CustomerAuthController {

    private final CustomerAuthService service;

    public CustomerAuthController(CustomerAuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(service.login(req.getUsername(), req.getPassword()));
    }

    /** 顾客公开注册；成功后直接返回登录 Token。 */
    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody CustomerRegisterRequest req) {
        return ApiResponse.success(service.register(req));
    }
}
