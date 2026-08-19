package com.orderflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 手机扫码后可访问的项目地址，不包含 API 路径。 */
@Data
@Component
@ConfigurationProperties(prefix = "orderflow.payment.mock")
public class MockPaymentProperties {
    private String publicBaseUrl = "http://localhost:8088";
}
