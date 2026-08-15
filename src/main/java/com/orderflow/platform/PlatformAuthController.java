package com.orderflow.platform;

import com.orderflow.auth.LoginRequest;
import com.orderflow.auth.TokenResponse;
import com.orderflow.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/auth")
public class PlatformAuthController {

    private final PlatformAuthService service;

    public PlatformAuthController(PlatformAuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest req) {
        return ApiResponse.success(service.login(req.getUsername(), req.getPassword()));
    }
}
