package com.orderflow.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdjustInventoryRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    @NotNull(message = "调整数量不能为空")
    private Long changeQuantity;
    private String reason;
}
