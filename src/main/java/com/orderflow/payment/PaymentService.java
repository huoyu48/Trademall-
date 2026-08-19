package com.orderflow.payment;

public interface PaymentService {
    MockCheckoutDTO createMockCheckout(Long orderId, Long customerId);

    PaymentStatusDTO getPaymentStatus(Long orderId, Long customerId);

    MockPaymentPageDTO getMockCheckoutPage(String paymentToken);

    MockPaymentPageDTO confirmMockPayment(String paymentToken);

    /** 订单取消后，本地关闭仍待支付的付款码。 */
    void closePendingPayments(Long orderId);
}
