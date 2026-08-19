package com.orderflow.payment;

import lombok.Data;

import java.time.LocalDateTime;

/** 手机模拟收银台展示与确认付款所需的数据。 */
@Data
public class MockPaymentPageDTO {
    private String paymentNo;
    private String orderNo;
    private Long amountCent;
    private String status;
    private boolean paid;
    private LocalDateTime expiresAt;
}
