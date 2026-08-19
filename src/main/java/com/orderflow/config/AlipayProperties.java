package com.orderflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本地环境变量注入的支付宝沙箱配置；私钥不进入仓库。 */
@Data
@Component
@ConfigurationProperties(prefix = "orderflow.payment.alipay")
public class AlipayProperties {
    private boolean enabled;
    private String appId;
    private String gatewayUrl;
    private String appPrivateKey;
    private String alipayPublicKey;
    private String notifyUrl;
    private String returnUrl;

    public boolean isReady() {
        return enabled && hasText(appId) && hasText(appPrivateKey)
                && hasText(alipayPublicKey) && hasText(notifyUrl);
    }

    /**
     * 支付完成后，支付宝会在浏览器中跳回该地址。沙箱联调中不能回跳到
     * http://localhost：支付宝的 HTTPS 收银台会把它当作不安全地址。
     */
    public String resolvedReturnUrl() {
        if (hasText(returnUrl) && returnUrl.startsWith("https://")) return returnUrl;
        if (!hasText(notifyUrl) || !notifyUrl.startsWith("https://")) return returnUrl;
        int callbackPath = notifyUrl.indexOf("/api/payments/alipay/notify");
        String publicBase = callbackPath >= 0 ? notifyUrl.substring(0, callbackPath) : notifyUrl;
        return publicBase + "/#/shop/orders";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
