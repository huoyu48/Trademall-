package com.orderflow.refund;

import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Refund;

public interface RefundService {
    Refund apply(Long orderId, String reason);

    /** 顾客跨商家订单申请退款：校验订单归属当前顾客后，按订单所属商家租户处理。 */
    Refund applyByCustomer(Long orderId, Long customerId, String reason);

    Refund approve(Long refundId);

    Refund reject(Long refundId);

    Refund detail(Long refundId);

    PageResult<Refund> page(int page, int size);
}
