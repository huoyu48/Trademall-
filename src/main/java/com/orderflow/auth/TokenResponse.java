package com.orderflow.auth;

import lombok.Data;

import java.util.List;

@Data
public class TokenResponse {
    private String token;
    private String tokenType = "Bearer";
    private long expiresInMinutes;
    private Long userId;
    private Long tenantId;
    private String tenantName;
    private String username;
    private List<String> roles;
}
