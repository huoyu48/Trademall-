package com.orderflow.product;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductDTO {
    private Long id;
    private String productCode;
    private String productName;
    private Long unitPriceCent;
    private Integer status;
    private Long categoryId;
    private String categoryName;
    private Long storeId;
    private String storeName;
    private Long sales;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
