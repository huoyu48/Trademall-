package com.orderflow.product;

import lombok.Data;

@Data
public class InventoryDTO {
    private Long productId;
    private String productName;
    private Long physicalQuantity;
    private Long reservedQuantity;
    private Long availableQuantity;
}
