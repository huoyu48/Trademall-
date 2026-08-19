package com.orderflow.payment;

import lombok.Data;

/** 电脑端展示模拟付款二维码所需的数据。 */
@Data
public class MockCheckoutDTO {
    private String paymentNo;
    private Long amountCent;
    private String qrCodeImage;
}
