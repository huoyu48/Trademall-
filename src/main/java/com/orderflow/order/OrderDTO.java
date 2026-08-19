package com.orderflow.order;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private String customerName;
    private Long customerId;
    private Long storeId;
    private String storeName;
    private String status;
    private Long totalAmountCent;
    private String promoCode;
    private Long discountAmountCent;
    private LocalDateTime createdAt;
    /** 商家可见的安全支付摘要；不返回付款码、回调原文等敏感字段。 */
    private PaymentInfoDTO payment;
    private List<OrderItemDTO> items;

    @Data
    public static class PaymentInfoDTO {
        private String paymentNo;
        private String provider;
        private String status;
        private Long amountCent;
        private LocalDateTime paidAt;
    }

    @Data
    public static class OrderItemDTO {
        private Long productId;
        private String productCode;
        private String productName;
        private Long unitPriceCent;
        private Integer quantity;
        private Long lineAmountCent;
    }
}
