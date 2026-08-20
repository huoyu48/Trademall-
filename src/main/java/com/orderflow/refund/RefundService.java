package com.orderflow.refund;

import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Refund;

public interface RefundService {
    Refund apply(Long orderId, String reason);

    /** 顾客跨商家订单申请退款：校验订单归属当前顾客后，按订单所属商家租户处理。 */
    Refund applyByCustomer(Long orderId, Long customerId, String reason);

    /** 顾客确认收货后的整单退货退款申请。 */
    Refund applyReturnByCustomer(Long orderId, Long customerId, String reason);

    /** 商家同意退货，顾客才能填写寄回物流单号。 */
    Refund approveReturn(Long refundId);

    /** 顾客填写已寄回商品的物流单号。 */
    Refund submitReturnShipmentByCustomer(Long refundId, Long customerId, String trackingNo);

    /** 商家确认收到退货。 */
    Refund receiveReturn(Long refundId);

    /** 商家确认收货后完成模拟退款和库存回补。 */
    Refund completeReturnRefund(Long refundId);

    Refund approve(Long refundId);

    Refund reject(Long refundId);

    Refund detail(Long refundId);

    PageResult<Refund> page(int page, int size);

    java.util.List<Refund> listByCustomer(Long customerId);
}
