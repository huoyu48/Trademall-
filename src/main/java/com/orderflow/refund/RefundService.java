package com.orderflow.refund;

import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Refund;

public interface RefundService {
    Refund apply(Long orderId, String reason);

    Refund approve(Long refundId);

    Refund reject(Long refundId);

    Refund detail(Long refundId);

    PageResult<Refund> page(int page, int size);
}
