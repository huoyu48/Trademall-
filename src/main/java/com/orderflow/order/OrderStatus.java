package com.orderflow.order;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：定义合法的状态流转。
 */
public enum OrderStatus {
    /** 顾客刚下单，库存已预占，等待付款。 */
    PENDING_PAYMENT,
    /** 顾客付款成功，等待商家确认。 */
    PAID,
    /** 兼容商家后台历史手工订单。 */
    CREATED,
    CONFIRMED,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    REFUNDING,
    REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, Set.of(PAID, CANCELLED),
            PAID, Set.of(CONFIRMED, CANCELLED),
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
