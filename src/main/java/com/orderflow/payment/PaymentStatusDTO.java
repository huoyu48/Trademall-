package com.orderflow.payment;

import lombok.Data;

/** 电脑端轮询订单付款状态所需的最小数据。 */
@Data
public class PaymentStatusDTO {
    private String orderStatus;
    private boolean paid;
}
