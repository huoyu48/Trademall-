package com.orderflow.category;

import lombok.Data;

@Data
class CreateCategoryRequest {
    private String categoryCode;
    private String categoryName;
    private Long parentId;
    private Integer sort;
    private Integer status;
}

@Data
class UpdateCategoryRequest {
    private String categoryName;
    private Long parentId;
    private Integer sort;
    private Integer status;
}
