package com.orderflow.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    /** 顾客端下单时回填的顾客ID，商家后台下单时为空 */
    private Long customerId;

    private String promoCode;

    @NotEmpty(message = "订单至少包含一个商品")
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotNull(message = "商品ID不能为空")
        private Long productId;
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须为正整数")
        private Integer quantity;
    }
}
