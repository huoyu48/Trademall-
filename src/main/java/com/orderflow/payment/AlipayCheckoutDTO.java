package com.orderflow.payment;

import lombok.Data;

/** 返回给商城的支付宝沙箱二维码；二维码内容由后端生成，前端不参与签名。 */
@Data
public class AlipayCheckoutDTO {
    private String paymentNo;
    private Long amountCent;
    private String qrCodeImage;
}
