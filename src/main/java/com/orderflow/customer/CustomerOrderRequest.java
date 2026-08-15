package com.orderflow.customer;

import com.orderflow.order.CreateOrderRequest;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CustomerOrderRequest {
    private String promoCode;

    @NotEmpty(message = "购物车不能为空")
    private List<CreateOrderRequest.OrderItemRequest> items;
}
