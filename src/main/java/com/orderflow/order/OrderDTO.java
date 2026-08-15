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
    private String status;
    private Long totalAmountCent;
    private String promoCode;
    private Long discountAmountCent;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;

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
