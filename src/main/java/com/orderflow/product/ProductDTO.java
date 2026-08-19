package com.orderflow.product;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    /** 顾客侧展示的本店当前有效满减活动，例如“每满 ¥500 减 ¥80”。 */
    private List<String> storePromotionTexts;
    private Long sales;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
