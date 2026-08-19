package com.orderflow.payment;

import com.orderflow.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手机扫码进入的公开模拟收银台接口。
 * <p>付款码 token 是一次性高随机值，且只能将对应的待付款订单推进到已付款；不接入任何真实资金渠道。</p>
 */
@RestController
@RequestMapping("/payments/mock")
public class MockPaymentController {

    private final PaymentService paymentService;

    public MockPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/checkout")
    public ApiResponse<MockPaymentPageDTO> checkout(@RequestParam String token) {
        return ApiResponse.success(paymentService.getMockCheckoutPage(token));
    }

    @PostMapping("/checkout/confirm")
    public ApiResponse<MockPaymentPageDTO> confirm(@RequestParam String token) {
        return ApiResponse.success(paymentService.confirmMockPayment(token));
    }
}
