package com.orderflow.auth;

public interface AuthService {
    TokenResponse login(LoginRequest request);
}
