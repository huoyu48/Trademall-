package com.orderflow.payment;

import lombok.Data;

/** 付款二维码弹窗轮询用的最小状态数据。 */
@Data
public class AlipayPaymentStatusDTO {
    private String orderStatus;
    private boolean paid;
}
