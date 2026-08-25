package com.orderflow.order;

import com.orderflow.refund.RefundStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderAndAfterSalesStateMachineTest {

    @Test
    void keepsPaymentAndFulfillmentInTheOrderStateMachine() {
        assertThat(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT,
                OrderStatus.PENDING_MERCHANT_CONFIRMATION)).isTrue();
        assertThat(OrderStatus.canTransition(OrderStatus.PENDING_MERCHANT_CONFIRMATION,
                OrderStatus.PENDING_SHIPMENT)).isTrue();
        assertThat(OrderStatus.canTransition(OrderStatus.PENDING_SHIPMENT,
                OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.canTransition(OrderStatus.SHIPPED,
                OrderStatus.COMPLETED)).isTrue();

        // 售后申请不是订单主状态，订单不能直接跳到退款处理中。
        assertThat(OrderStatus.canTransition(OrderStatus.COMPLETED,
                OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT,
                OrderStatus.REFUNDED)).isFalse();
    }

    @Test
    void keepsReturnLogisticsInTheAfterSalesStateMachine() {
        assertThat(RefundStatus.canTransition(RefundStatus.PENDING_REVIEW,
                RefundStatus.WAITING_CUSTOMER_RETURN)).isTrue();
        assertThat(RefundStatus.canTransition(RefundStatus.WAITING_CUSTOMER_RETURN,
                RefundStatus.WAITING_MERCHANT_RECEIPT)).isTrue();
        assertThat(RefundStatus.canTransition(RefundStatus.WAITING_MERCHANT_RECEIPT,
                RefundStatus.REFUNDING)).isTrue();
        assertThat(RefundStatus.canTransition(RefundStatus.REFUNDING,
                RefundStatus.REFUNDED)).isTrue();

        assertThat(RefundStatus.canTransition(RefundStatus.PENDING_REVIEW,
                RefundStatus.REFUNDED)).isFalse();
    }
}
