package com.orderflow.order;

import lombok.Data;

/** 顾客提交订单前的服务端结算预览；金额仍以下单时的后端计算为准。 */
@Data
public class OrderPricingDTO {
    private Long storeId;
    private String storeName;
    private Long subtotalAmountCent;
    private String promoCode;
    private Long discountAmountCent;
    private Long payableAmountCent;
}
