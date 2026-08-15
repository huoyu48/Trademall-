package com.orderflow.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class CreateProductRequest {
    @NotBlank(message = "商品编码不能为空")
    private String productCode;
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    @NotNull(message = "单价不能为空")
    @PositiveOrZero(message = "单价必须为非负整数（分）")
    private Long unitPriceCent;
    private Integer status = 1;
}
