package com.orderflow.refund;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.entity.Refund;
import com.orderflow.domain.mapper.OrdersMapper;
import com.orderflow.domain.mapper.PaymentTransactionMapper;
import com.orderflow.domain.mapper.RefundMapper;
import com.orderflow.order.OrderService;
import com.orderflow.order.OrderStatus;
import com.orderflow.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class RefundServiceImpl implements RefundService {

    private final RefundMapper refundMapper;
    private final OrdersMapper ordersMapper;
    private final OrderService orderService;
    private final PaymentTransactionMapper paymentMapper;

    public RefundServiceImpl(RefundMapper refundMapper, OrdersMapper ordersMapper, OrderService orderService,
                             PaymentTransactionMapper paymentMapper) {
        this.refundMapper = refundMapper;
        this.ordersMapper = ordersMapper;
        this.orderService = orderService;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public Refund apply(Long orderId, String reason) {
        Long tenantId = TenantContext.getTenantId();
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !tenantId.equals(order.getTenantId())) {
            throw new BizException(BizErrorCode.ORDER_NOT_IN_TENANT);
        }
        OrderStatus current = OrderStatus.valueOf(order.getStatus());
        if (current != OrderStatus.PAID && current != OrderStatus.CONFIRMED
                && current != OrderStatus.SHIPPED && current != OrderStatus.COMPLETED) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        return createRefund(order, tenantId, reason, "REFUND_ONLY");
    }

    @Override
    @Transactional
    public Refund applyReturnByCustomer(Long orderId, Long customerId, String reason) {
        return withCustomerOrder(orderId, customerId, order -> {
            if (OrderStatus.valueOf(order.getStatus()) != OrderStatus.COMPLETED) {
                throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
            }
            return createRefund(order, order.getTenantId(), reason, "RETURN_REFUND");
        });
    }

    private Refund createRefund(Orders order, Long tenantId, String reason, String afterSaleType) {
        OrderStatus current = OrderStatus.valueOf(order.getStatus());
        Refund r = new Refund();
        r.setTenantId(tenantId);
        r.setRefundNo("RF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        r.setOrderId(order.getId());
        r.setOrderNo(order.getOrderNo());
        r.setReason(reason == null || reason.isBlank() ? "顾客售后申请" : reason.trim());
        r.setRefundAmountCent(order.getTotalAmountCent());
        r.setStatus("PENDING");
        r.setAfterSaleType(afterSaleType);
        r.setOriginalOrderStatus(current.name());
        refundMapper.insert(r);
        orderService.applyRefund(order.getId());
        return r;
    }

    @Override
    @Transactional
    public Refund applyByCustomer(Long orderId, Long customerId, String reason) {
        return withCustomerOrder(orderId, customerId,
                order -> apply(order.getId(), reason == null || reason.isBlank() ? "顾客申请退款" : reason.trim()));
    }

    @Override
    @Transactional
    public Refund submitReturnShipmentByCustomer(Long refundId, Long customerId, String trackingNo) {
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new BizException(40003, "请填写退货物流单号");
        }
        Long previousTenantId = TenantContext.getTenantId();
        Long previousUserId = TenantContext.getUserId();
        String previousUsername = TenantContext.getUsername();
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Refund refund = refundMapper.selectById(refundId);
            Orders order = refund == null ? null : ordersMapper.selectById(refund.getOrderId());
            if (order == null || !Objects.equals(customerId, order.getCustomerId())) {
                throw new BizException(BizErrorCode.ORDER_NOT_IN_TENANT);
            }
            TenantContext.set(order.getTenantId(), customerId, previousUsername);
            TenantContext.setIgnoreTenant(false);
            Refund current = require(refundId, order.getTenantId());
            if (!"RETURN_REFUND".equals(current.getAfterSaleType()) || !"RETURN_APPROVED".equals(current.getStatus())) {
                throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
            }
            current.setReturnTrackingNo(trackingNo.trim());
            current.setReturnedAt(LocalDateTime.now());
            current.setStatus("RETURNING");
            refundMapper.updateById(current);
            return current;
        } finally {
            TenantContext.set(previousTenantId, previousUserId, previousUsername);
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    @Transactional
    public Refund approve(Long refundId) {
        Long tenantId = TenantContext.getTenantId();
        Refund r = require(refundId, tenantId);
        if (!"REFUND_ONLY".equals(r.getAfterSaleType()) || !"PENDING".equals(r.getStatus())) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        r.setStatus("REFUNDED");
        refundMapper.updateById(r);
        orderService.finishRefund(r.getOrderId(), OrderStatus.valueOf(r.getOriginalOrderStatus()));
        paymentMapper.markRefundedByOrderId(r.getOrderId());
        return r;
    }

    @Override
    @Transactional
    public Refund approveReturn(Long refundId) {
        Refund r = require(refundId, TenantContext.getTenantId());
        if (!"RETURN_REFUND".equals(r.getAfterSaleType()) || !"PENDING".equals(r.getStatus())) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        r.setStatus("RETURN_APPROVED");
        r.setReturnApprovedAt(LocalDateTime.now());
        refundMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional
    public Refund receiveReturn(Long refundId) {
        Refund r = require(refundId, TenantContext.getTenantId());
        if (!"RETURN_REFUND".equals(r.getAfterSaleType()) || !"RETURNING".equals(r.getStatus())) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        r.setStatus("RETURN_RECEIVED");
        r.setReceivedAt(LocalDateTime.now());
        refundMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional
    public Refund completeReturnRefund(Long refundId) {
        Refund r = require(refundId, TenantContext.getTenantId());
        if (!"RETURN_REFUND".equals(r.getAfterSaleType()) || !"RETURN_RECEIVED".equals(r.getStatus())) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        r.setStatus("REFUNDED");
        refundMapper.updateById(r);
        orderService.finishReturnRefund(r.getOrderId());
        paymentMapper.markRefundedByOrderId(r.getOrderId());
        return r;
    }

    @Override
    @Transactional
    public Refund reject(Long refundId) {
        Long tenantId = TenantContext.getTenantId();
        Refund r = require(refundId, tenantId);
        if (!"PENDING".equals(r.getStatus()) && !"RETURN_APPROVED".equals(r.getStatus())) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        r.setStatus("REJECTED");
        refundMapper.updateById(r);
        OrderStatus originalStatus = r.getOriginalOrderStatus() == null
                ? OrderStatus.SHIPPED : OrderStatus.valueOf(r.getOriginalOrderStatus());
        orderService.closeRefund(r.getOrderId(), originalStatus);
        return r;
    }

    @Override
    public Refund detail(Long refundId) {
        return require(refundId, TenantContext.getTenantId());
    }

    @Override
    public PageResult<Refund> page(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        Page<Refund> p = refundMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<Refund>().eq("tenant_id", tenantId).orderByDesc("id"));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Override
    public java.util.List<Refund> listByCustomer(Long customerId) {
        return refundMapper.findByCustomerId(customerId);
    }

    private <T> T withCustomerOrder(Long orderId, Long customerId, java.util.function.Function<Orders, T> action) {
        Long previousTenantId = TenantContext.getTenantId();
        Long previousUserId = TenantContext.getUserId();
        String previousUsername = TenantContext.getUsername();
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            // 顾客“我的订单”允许跨商家展示；先跨租户读取，再按顾客身份校验。
            TenantContext.setIgnoreTenant(true);
            Orders order = ordersMapper.selectById(orderId);
            if (order == null || !Objects.equals(customerId, order.getCustomerId())) {
                throw new BizException(BizErrorCode.ORDER_NOT_IN_TENANT);
            }
            // 退款单、状态历史与库存操作必须写入订单所属商家租户。
            TenantContext.set(order.getTenantId(), customerId, previousUsername);
            TenantContext.setIgnoreTenant(false);
            return action.apply(order);
        } finally {
            TenantContext.set(previousTenantId, previousUserId, previousUsername);
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    private Refund require(Long id, Long tenantId) {
        Refund r = refundMapper.selectById(id);
        if (r == null || !tenantId.equals(r.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        return r;
    }
}
