package com.orderflow.order;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceImplLockKeyTest {

    @Test
    void sortsAndDeduplicatesProductIdsBeforeAcquiringLocks() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item(9L), item(3L), item(9L), item(5L)));

        assertThat(OrderServiceImpl.orderedProductIds(request)).containsExactly(3L, 5L, 9L);
    }

    @Test
    void lockKeyIncludesBothTenantAndProduct() {
        assertThat(OrderServiceImpl.productLockKey(7L, 42L))
                .isEqualTo("order:create:lock:7:product:42");
    }

    private CreateOrderRequest.OrderItemRequest item(Long productId) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);
        return item;
    }
}
