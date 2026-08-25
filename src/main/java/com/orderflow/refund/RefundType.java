package com.orderflow.refund;

/** 仅退款与退货退款必须分开建模，决定后续允许的售后动作。 */
public enum RefundType {
    REFUND_ONLY,
    RETURN_AND_REFUND
}
