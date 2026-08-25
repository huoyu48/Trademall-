package com.orderflow.refund;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.entity.OrderItem;
import com.orderflow.domain.entity.Refund;
import com.orderflow.domain.mapper.InventoryMapper;
import com.orderflow.domain.mapper.OrderItemMapper;
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
    private final OrderItemMapper orderItemMapper;
    private final InventoryMapper inventoryMapper;

    public RefundServiceImpl(RefundMapper refundMapper, OrdersMapper ordersMapper, OrderService orderService,
                             PaymentTransactionMapper paymentMapper, OrderItemMapper orderItemMapper,
                             InventoryMapper inventoryMapper) {
        this.refundMapper = refundMapper;
        this.ordersMapper = ordersMapper;
        this.orderService = orderService;
        this.paymentMapper = paymentMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryMapper = inventoryMapper;
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
        Refund latest = refundMapper.findLatestByOrderId(orderId);
        if (latest != null && !RefundStatus.REJECTED.name().equals(latest.getStatus())
                && !RefundStatus.REFUNDED.name().equals(latest.getStatus())) {
            throw new BizException(40914, "该订单已有处理中售后申请");
        }

        RefundType type;
        if (current == OrderStatus.PENDING_MERCHANT_CONFIRMATION || current == OrderStatus.PENDING_SHIPMENT) {
            type = RefundType.REFUND_ONLY;
        } else if (current == OrderStatus.COMPLETED) {
            type = RefundType.RETURN_AND_REFUND;
        } else {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        Refund r = new Refund();
        r.setTenantId(tenantId);
        r.setRefundNo("RF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        r.setOrderId(orderId);
        r.setOrderNo(order.getOrderNo());
        r.setReason(reason);
        r.setRefundAmountCent(order.getTotalAmountCent());
        r.setRefundType(type.name());
        r.setStatus(RefundStatus.PENDING_REVIEW.name());
        refundMapper.insert(r);
        return r;
    }

    @Override
    @Transactional
    public Refund applyByCustomer(Long orderId, Long customerId, String reason) {
        Long previousTenantId = TenantContext.getTenantId();
        Long previousUserId = TenantContext.getUserId();
        String previousUsername = TenantContext.getUsername();
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            // 顾客“我的订单”允许跨商家展示；先跨租户读取订单，再按顾客身份校验。
            TenantContext.setIgnoreTenant(true);
            Orders order = ordersMapper.selectById(orderId);
            if (order == null || !Objects.equals(customerId, order.getCustomerId())) {
                throw new BizException(BizErrorCode.ORDER_NOT_IN_TENANT);
            }

            // 退款单、状态历史与库存操作必须写入订单所属商家租户。
            TenantContext.set(order.getTenantId(), customerId, previousUsername);
            TenantContext.setIgnoreTenant(false);
            return apply(orderId, reason == null || reason.isBlank() ? "顾客申请退款" : reason.trim());
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
        RefundStatus target = RefundType.RETURN_AND_REFUND.name().equals(r.getRefundType())
                ? RefundStatus.WAITING_CUSTOMER_RETURN : RefundStatus.REFUNDING;
        transition(r, tenantId, target);
        return r;
    }

    @Override
    @Transactional
    public Refund reject(Long refundId) {
        Long tenantId = TenantContext.getTenantId();
        Refund r = require(refundId, tenantId);
        transition(r, tenantId, RefundStatus.REJECTED);
        return r;
    }

    @Override
    @Transactional
    public Refund submitReturnLogisticsByCustomer(Long refundId, Long customerId, String logisticsCompany, String trackingNo) {
        if (isBlank(logisticsCompany) || isBlank(trackingNo)) {
            throw new BizException(40001, "请填写物流公司和退货单号");
        }
        Long previousTenantId = TenantContext.getTenantId();
        Long previousUserId = TenantContext.getUserId();
        String previousUsername = TenantContext.getUsername();
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Refund r = refundMapper.selectById(refundId);
            if (r == null) throw new BizException(BizErrorCode.NOT_FOUND);
            Orders order = ordersMapper.selectById(r.getOrderId());
            if (order == null || !Objects.equals(order.getCustomerId(), customerId)) {
                throw new BizException(BizErrorCode.ORDER_NOT_IN_TENANT);
            }
            TenantContext.set(order.getTenantId(), customerId, previousUsername);
            TenantContext.setIgnoreTenant(false);
            if (RefundType.RETURN_AND_REFUND.name().equals(r.getRefundType())
                    && RefundStatus.WAITING_CUSTOMER_RETURN.name().equals(r.getStatus())) {
                r.setReturnLogisticsCompany(logisticsCompany.trim());
                r.setReturnTrackingNo(trackingNo.trim());
                r.setReturnShippedAt(LocalDateTime.now());
                refundMapper.updateById(r);
                transition(r, order.getTenantId(), RefundStatus.WAITING_MERCHANT_RECEIPT);
                return r;
            }
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        } finally {
            TenantContext.set(previousTenantId, previousUserId, previousUsername);
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    @Transactional
    public Refund confirmReturnReceived(Long refundId) {
        Long tenantId = TenantContext.getTenantId();
        Refund r = require(refundId, tenantId);
        transition(r, tenantId, RefundStatus.REFUNDING);
        // 已发货商品先在发货时扣减实体库存；商家确认收到退货后才重新入库。
        for (OrderItem item : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", r.getOrderId()))) {
            if (inventoryMapper.restock(tenantId, item.getProductId(), item.getQuantity()) != 1) {
                throw new BizException(50001, "退货入库失败");
            }
        }
        r.setMerchantReceivedAt(LocalDateTime.now());
        refundMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional
    public Refund completeRefund(Long refundId) {
        Long tenantId = TenantContext.getTenantId();
        Refund r = require(refundId, tenantId);
        transition(r, tenantId, RefundStatus.REFUNDED);
        orderService.markRefunded(r.getOrderId());
        paymentMapper.markRefundedByOrderId(r.getOrderId());
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

    private Refund require(Long id, Long tenantId) {
        Refund r = refundMapper.selectById(id);
        if (r == null || !tenantId.equals(r.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        return r;
    }

    private void transition(Refund refund, Long tenantId, RefundStatus target) {
        RefundStatus current = RefundStatus.valueOf(refund.getStatus());
        if (!RefundStatus.canTransition(current, target)
                || refundMapper.transitionStatus(refund.getId(), tenantId, current.name(), target.name()) != 1) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        refund.setStatus(target.name());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
