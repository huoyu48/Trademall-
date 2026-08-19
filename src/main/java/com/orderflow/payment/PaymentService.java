package com.orderflow.payment;

import com.orderflow.domain.entity.Orders;

import java.util.Map;

public interface PaymentService {
    AlipayCheckoutDTO createAlipayCheckout(Long orderId, Long customerId);

    AlipayPaymentStatusDTO getAlipayPaymentStatus(Long orderId, Long customerId);

    /** 支付宝异步通知只在验签、金额及订单校验均通过后才会更新订单。 */
    boolean handleAlipayNotify(Map<String, String> parameters);

    /** 本地订单超时取消后，尽力关闭支付宝侧仍待支付的交易。 */
    void closePendingAlipayPayment(Orders order);

    /** 顾客手动取消订单后，按订单号关闭其仍待支付的支付宝交易。 */
    void closePendingAlipayPayment(Long orderId);
}
