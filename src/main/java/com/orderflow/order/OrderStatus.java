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
            // 已付款后不能再直接取消；商家无法履约时必须进入退款流程，保留资金记录。
            PAID, Set.of(CONFIRMED, REFUNDING),
            CREATED, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(SHIPPED, REFUNDING),
            SHIPPED, Set.of(COMPLETED, REFUNDING),
            COMPLETED, Set.of(REFUNDING),
            // 驳回退款申请后返回申请前状态；退款成功才进入终态 REFUNDED。
            REFUNDING, Set.of(PAID, CONFIRMED, SHIPPED, COMPLETED, REFUNDED)
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
