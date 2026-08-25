package com.orderflow.order;

import java.util.Map;
import java.util.Set;

/** 订单履约状态机：售后进度由退款单单独记录，不能混入订单主状态。 */
public enum OrderStatus {
    /** 顾客刚下单，库存已预占，等待付款。 */
    PENDING_PAYMENT,
    /** 已付款，等待商家确认是否接单。 */
    PENDING_MERCHANT_CONFIRMATION,
    /** 商家已确认，等待发货。 */
    PENDING_SHIPMENT,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, Set.of(PENDING_MERCHANT_CONFIRMATION, CANCELLED),
            PENDING_MERCHANT_CONFIRMATION, Set.of(PENDING_SHIPMENT, REFUNDED),
            PENDING_SHIPMENT, Set.of(SHIPPED, REFUNDED),
            SHIPPED, Set.of(COMPLETED),
            // 已完成的退货退款在售后单走完“审核、寄回、收货、退款”后才进入已退款。
            COMPLETED, Set.of(REFUNDED)
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
