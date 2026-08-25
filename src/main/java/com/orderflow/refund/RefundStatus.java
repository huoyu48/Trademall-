package com.orderflow.refund;

import java.util.Map;
import java.util.Set;

/** 售后单状态机：退款与退货退款的过程只记录在售后单中。 */
public enum RefundStatus {
    PENDING_REVIEW,
    WAITING_CUSTOMER_RETURN,
    WAITING_MERCHANT_RECEIPT,
    REFUNDING,
    REFUNDED,
    REJECTED;

    private static final Map<RefundStatus, Set<RefundStatus>> TRANSITIONS = Map.of(
            PENDING_REVIEW, Set.of(WAITING_CUSTOMER_RETURN, REFUNDING, REJECTED),
            WAITING_CUSTOMER_RETURN, Set.of(WAITING_MERCHANT_RECEIPT),
            WAITING_MERCHANT_RECEIPT, Set.of(REFUNDING),
            REFUNDING, Set.of(REFUNDED)
    );

    public static boolean canTransition(RefundStatus from, RefundStatus to) {
        return from != null && to != null && from != to
                && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
