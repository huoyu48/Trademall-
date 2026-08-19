package com.orderflow.payment;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 支付宝服务器回调入口：不能依赖浏览器 return_url，更不能要求 JWT。 */
@RestController
@RequestMapping("/payments/alipay")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String notify(@RequestParam LinkedMultiValueMap<String, String> body) {
        Map<String, String> parameters = new LinkedHashMap<>();
        body.forEach((key, values) -> parameters.put(key, values == null || values.isEmpty() ? null : values.get(0)));
        return paymentService.handleAlipayNotify(parameters) ? "success" : "failure";
    }
}
