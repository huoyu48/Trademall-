package com.orderflow.order;

import com.orderflow.common.PageResult;

import java.util.List;

public interface OrderService {
    OrderDTO create(CreateOrderRequest request, String idempotencyKey);

    /**
     * 顾客端下单用：租户身份来自商品而非当前登录身份，
     * 顾客可跨租户下单，订单自动归属商品所在租户。
     */
    OrderDTO create(CreateOrderRequest request, String idempotencyKey, Long explicitTenantId);

    /** 顾客购物车预览：按商品所属商家计算店铺满减，但不创建订单、不扣库存。 */
    OrderPricingDTO preview(CreateOrderRequest request, Long explicitTenantId);

    OrderDTO get(Long orderId);

    PageResult<OrderDTO> page(int page, int size);

    PageResult<OrderDTO> page(int page, int size, String status, String orderNo);

    OrderDTO confirm(Long orderId);

    OrderDTO ship(Long orderId);

    OrderDTO complete(Long orderId);

    OrderDTO cancel(Long orderId);

    /** 顾客仅能取消自己仍未付款的订单；取消后回补预占库存。 */
    OrderDTO cancelPendingPaymentByCustomer(Long orderId, Long customerId);

    List<OrderStatusHistoryDTO> getHistory(Long orderId);

    OrderStatsDTO stats();

    OrderDTO applyRefund(Long orderId);

    OrderDTO finishRefund(Long orderId);

    OrderDTO closeRefund(Long orderId);

    /** 顾客端：查询某顾客在本租户下的全部订单 */
    List<OrderDTO> listByCustomer(Long customerId);
}
