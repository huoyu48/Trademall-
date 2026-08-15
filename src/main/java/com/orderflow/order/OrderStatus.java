package com.orderflow.order;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：定义合法的状态流转。
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    REFUNDING,
    REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            CREATED, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(COMPLETED, REFUNDING),
            COMPLETED, Set.of(REFUNDING),
            REFUNDING, Set.of(REFUNDED, CANCELLED)
    );

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
