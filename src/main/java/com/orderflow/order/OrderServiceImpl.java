package com.orderflow.order;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.audit.AuditLogService;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.common.RedisLockService;
import com.orderflow.config.RabbitMQConfig;
import com.orderflow.domain.entity.Inventory;
import com.orderflow.domain.entity.OrderItem;
import com.orderflow.domain.entity.OrderStatusHistory;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.entity.Product;
import com.orderflow.domain.entity.Promotion;
import com.orderflow.domain.entity.Store;
import com.orderflow.domain.mapper.PromotionMapper;
import com.orderflow.domain.mapper.InventoryMapper;
import com.orderflow.domain.mapper.OrderItemMapper;
import com.orderflow.domain.mapper.OrderStatusHistoryMapper;
import com.orderflow.domain.mapper.OrdersMapper;
import com.orderflow.domain.mapper.ProductMapper;
import com.orderflow.domain.mapper.StoreMapper;
import com.orderflow.outbox.OutboxEventService;
import com.orderflow.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final long PRODUCT_LOCK_WAIT_MILLIS = 3000;
    private static final long PRODUCT_LOCK_TTL_MILLIS = 5000;

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final AuditLogService auditLogService;
    private final OutboxEventService outboxEventService;
    private final RedisLockService redisLock;
    private final RabbitTemplate rabbitTemplate;
    private final PromotionMapper promotionMapper;
    private final StoreMapper storeMapper;

    @Value("${orderflow.inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    public OrderServiceImpl(OrdersMapper ordersMapper, OrderItemMapper orderItemMapper,
                            OrderStatusHistoryMapper historyMapper, ProductMapper productMapper,
                            InventoryMapper inventoryMapper, AuditLogService auditLogService,
                            OutboxEventService outboxEventService, RedisLockService redisLock,
                            RabbitTemplate rabbitTemplate, PromotionMapper promotionMapper,
                            StoreMapper storeMapper) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.historyMapper = historyMapper;
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.auditLogService = auditLogService;
        this.outboxEventService = outboxEventService;
        this.redisLock = redisLock;
        this.rabbitTemplate = rabbitTemplate;
        this.promotionMapper = promotionMapper;
        this.storeMapper = storeMapper;
    }

    @Override
    @Transactional
    public OrderDTO create(CreateOrderRequest request, String idempotencyKey) {
        return create(request, idempotencyKey, null);
    }

    @Override
    @Transactional
    public OrderDTO create(CreateOrderRequest request, String idempotencyKey, Long explicitTenantId) {
        Long tenantId = explicitTenantId != null ? explicitTenantId : TenantContext.getTenantId();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BizException(BizErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        // 只锁本订单涉及的商品，不再用租户级全局锁阻塞同商家其他商品的下单。
        // 多商品订单按 productId 升序加锁，所有请求遵循同一顺序以避免相互等待。
        List<ProductLock> productLocks = acquireProductLocks(tenantId, request);
        boolean releaseAfterTransaction = registerReleaseAfterTransaction(productLocks);

        // 跨租户下单（顾客买别家商品）时，doCreate 内的 selectById/selectList 会自动注入
        // tenant_id=当前顾客的 t-a，导致跨租户商品查不到。临时放开租户拦截。
        boolean crossTenant = explicitTenantId != null && !explicitTenantId.equals(TenantContext.getTenantId());
        if (crossTenant) TenantContext.setIgnoreTenant(true);
        try {
            return doCreate(request, idempotencyKey, tenantId);
        } finally {
            if (crossTenant) TenantContext.setIgnoreTenant(false);
            // 事务提交/回滚后再释放，避免锁已放开但库存、订单尚未提交。
            if (!releaseAfterTransaction) {
                releaseProductLocks(productLocks);
            }
        }
    }

    @Override
    public OrderPricingDTO preview(CreateOrderRequest request, Long explicitTenantId) {
        Long tenantId = explicitTenantId != null ? explicitTenantId : TenantContext.getTenantId();
        boolean crossTenant = explicitTenantId != null && !explicitTenantId.equals(TenantContext.getTenantId());
        if (crossTenant) TenantContext.setIgnoreTenant(true);
        try {
            long subtotal = 0L;
            Long storeId = null;
            String storeName = null;
            boolean firstProduct = true;
            for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
                Product product = productMapper.selectById(item.getProductId());
                if (product == null || !tenantId.equals(product.getTenantId())) {
                    throw new BizException(BizErrorCode.PRODUCT_NOT_IN_TENANT);
                }
                if (product.getStatus() == 0) throw new BizException(BizErrorCode.PRODUCT_DISABLED);
                if (firstProduct) {
                    storeId = product.getStoreId();
                    firstProduct = false;
                    if (storeId != null) {
                        Store store = storeMapper.selectById(storeId);
                        if (store == null || !tenantId.equals(store.getTenantId())) {
                            throw new BizException(BizErrorCode.PRODUCT_NOT_IN_TENANT);
                        }
                        storeName = store.getStoreName();
                    }
                } else if (!Objects.equals(storeId, product.getStoreId())) {
                    throw new BizException(40003, "一次订单只能购买同一家门店的商品");
                }
                subtotal += product.getUnitPriceCent() * item.getQuantity();
            }
            Promotion promo = findApplicablePromotion(tenantId, request.getPromoCode(), subtotal, LocalDateTime.now());
            long discount = calculateDiscount(promo, subtotal);
            OrderPricingDTO result = new OrderPricingDTO();
            result.setStoreId(storeId);
            result.setStoreName(storeName);
            result.setSubtotalAmountCent(subtotal);
            result.setPromoCode(promo == null ? null : promo.getPromoCode());
            result.setDiscountAmountCent(discount);
            result.setPayableAmountCent(subtotal - discount);
            return result;
        } finally {
            if (crossTenant) TenantContext.setIgnoreTenant(false);
        }
    }

    static List<Long> orderedProductIds(CreateOrderRequest request) {
        return request.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getProductId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    static String productLockKey(Long tenantId, Long productId) {
        return "order:create:lock:" + tenantId + ":product:" + productId;
    }

    private List<ProductLock> acquireProductLocks(Long tenantId, CreateOrderRequest request) {
        long deadline = System.currentTimeMillis() + PRODUCT_LOCK_WAIT_MILLIS;
        List<ProductLock> acquired = new ArrayList<>();
        for (Long productId : orderedProductIds(request)) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining < 0) {
                releaseProductLocks(acquired);
                throw new BizException(BizErrorCode.LOCK_ACQUIRE_FAILED);
            }
            String key = productLockKey(tenantId, productId);
            String value = redisLock.tryLock(key, remaining, PRODUCT_LOCK_TTL_MILLIS);
            if (value == null) {
                releaseProductLocks(acquired);
                throw new BizException(BizErrorCode.LOCK_ACQUIRE_FAILED);
            }
            acquired.add(new ProductLock(key, value));
        }
        return acquired;
    }

    private boolean registerReleaseAfterTransaction(List<ProductLock> productLocks) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseProductLocks(productLocks);
            }
        });
        return true;
    }

    private void releaseProductLocks(List<ProductLock> productLocks) {
        for (int i = productLocks.size() - 1; i >= 0; i--) {
            ProductLock lock = productLocks.get(i);
            redisLock.release(lock.key(), lock.value());
        }
    }

    private record ProductLock(String key, String value) {
    }

    private OrderDTO doCreate(CreateOrderRequest request, String idempotencyKey, Long tenantId) {
        // 幂等：先查旧订单
        Orders existing = ordersMapper.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existing != null) {
            return toDTO(existing);
        }

        // 校验商品并取快照
        List<OrderItem> items = new ArrayList<>();
        long total = 0;
        Long storeId = null;
        String storeNameSnapshot = null;
        boolean firstProduct = true;
        for (CreateOrderRequest.OrderItemRequest it : request.getItems()) {
            Product p = productMapper.selectById(it.getProductId());
            if (p == null || !tenantId.equals(p.getTenantId())) {
                throw new BizException(BizErrorCode.PRODUCT_NOT_IN_TENANT);
            }
            if (p.getStatus() == 0) {
                throw new BizException(BizErrorCode.PRODUCT_DISABLED);
            }
            // 顾客端已按门店拆单；后端再次校验，不能依赖前端分组结果。
            if (firstProduct) {
                storeId = p.getStoreId();
                firstProduct = false;
                if (storeId != null) {
                    Store store = storeMapper.selectById(storeId);
                    if (store == null || !tenantId.equals(store.getTenantId())) {
                        throw new BizException(BizErrorCode.PRODUCT_NOT_IN_TENANT);
                    }
                    storeNameSnapshot = store.getStoreName();
                }
            } else if (!Objects.equals(storeId, p.getStoreId())) {
                throw new BizException(40003, "一次订单只能购买同一家门店的商品");
            }
            OrderItem oi = new OrderItem();
            oi.setProductId(p.getId());
            oi.setProductCodeSnapshot(p.getProductCode());
            oi.setProductNameSnapshot(p.getProductName());
            oi.setUnitPriceCent(p.getUnitPriceCent());
            oi.setQuantity(it.getQuantity());
            oi.setLineAmountCent(p.getUnitPriceCent() * it.getQuantity());
            items.add(oi);
            total += oi.getLineAmountCent();
        }

        // 未手动选券时，自动使用该商家当前可用且优惠力度最大的满减；
        // 显式传入活动码时则校验并使用该活动。最终金额只由后端计算。
        String appliedPromo = null;
        long discount = 0L;
        Promotion promo = findApplicablePromotion(tenantId, request.getPromoCode(), total, LocalDateTime.now());
        if (promo != null) {
            discount = calculateDiscount(promo, total);
            appliedPromo = promo.getPromoCode();
        }
        total -= discount;

        Orders order = new Orders();
        order.setTenantId(tenantId);
        order.setOrderNo(generateOrderNo());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerId(request.getCustomerId());
        order.setStoreId(storeId);
        order.setStoreNameSnapshot(storeNameSnapshot);
        order.setStatus(OrderStatus.CREATED.name());
        order.setPromoCode(appliedPromo);
        order.setDiscountAmountCent(discount > 0 ? discount : null);
        order.setTotalAmountCent(total);
        order.setIdempotencyKey(idempotencyKey);
        order.setCreatedBy(TenantContext.getUserId());

        try {
            ordersMapper.insert(order);
        } catch (DuplicateKeyException e) {
            Orders dup = ordersMapper.findByIdempotencyKey(tenantId, idempotencyKey);
            if (dup != null) {
                return toDTO(dup);
            }
            throw new BizException(BizErrorCode.IDEMPOTENCY_CONFLICT);
        }

        // 并发安全的库存预占；预占后可用量低于阈值则发布低库存告警事件
        for (OrderItem oi : items) {
            int rows = inventoryMapper.reserve(tenantId, oi.getProductId(), oi.getQuantity());
            if (rows == 0) {
                throw new BizException(BizErrorCode.INSUFFICIENT_INVENTORY);
            }
            Inventory inv = inventoryMapper.selectByProduct(tenantId, oi.getProductId());
            long available = inv.getPhysicalQuantity() - inv.getReservedQuantity();
            if (available <= lowStockThreshold) {
                publishLowStock(tenantId, oi.getProductId(), available);
            }
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);
        }

        insertHistory(order.getId(), tenantId, null, OrderStatus.CREATED.name(), "创建订单");
        auditLogService.write("CREATE_ORDER", "order", String.valueOf(order.getId()),
                null, "orderNo=" + order.getOrderNo());

        // 与订单在同一事务内写入 Outbox；后台任务收到 Broker 确认后才标记为已发送。
        List<OrderEventMessage.OrderEventItem> eventItems = items.stream().map(oi ->
                OrderEventMessage.OrderEventItem.builder()
                        .productId(oi.getProductId())
                        .quantity(oi.getQuantity())
                        .build()).toList();
        outboxEventService.recordOrderCreated(order, eventItems, org.slf4j.MDC.get("requestId"));

        return toDTO(order);
    }

    private Promotion findApplicablePromotion(Long tenantId, String promoCode, long total, LocalDateTime now) {
        QueryWrapper<Promotion> query = new QueryWrapper<Promotion>()
                .eq("tenant_id", tenantId)
                .eq("status", 1);
        if (promoCode != null && !promoCode.isBlank()) {
            query.eq("promo_code", promoCode);
        } else {
            // 没有选券时只自动用满减；优惠券还需要后续的领券/选券能力，不能擅自给用户使用。
            query.eq("promo_type", "FULL_REDUCTION");
        }
        return chooseBestPromotion(promotionMapper.selectList(query), total, now);
    }

    static Promotion chooseBestPromotion(List<Promotion> promotions, long total, LocalDateTime now) {
        if (promotions == null) return null;
        return promotions.stream()
                .filter(p -> isApplicable(p, total, now))
                .max(Comparator.comparingLong(p -> calculateDiscount(p, total)))
                .orElse(null);
    }

    /**
     * 满减按门槛倍数循环计算，例如每满 500 减 80，3499 元应减 6 * 80 = 480 元。
     * 优惠券、首单立减等其他活动仍然只使用一次。历史数据中负数减免额也兼容为正数。
     */
    static long calculateDiscount(Promotion promotion, long total) {
        if (promotion == null || total <= 0 || promotion.getDiscountAmountCent() == null) return 0L;
        long perDiscount = Math.abs(promotion.getDiscountAmountCent());
        long threshold = promotion.getThresholdCent() == null ? 0L : promotion.getThresholdCent();
        long discount = perDiscount;
        if ("FULL_REDUCTION".equals(promotion.getPromoType()) && threshold > 0) {
            discount = (total / threshold) * perDiscount;
        }
        return Math.min(total, discount);
    }

    private static boolean isApplicable(Promotion promotion, long total, LocalDateTime now) {
        if (promotion == null || promotion.getDiscountAmountCent() == null
                || promotion.getDiscountAmountCent() == 0
                || promotion.getStatus() == null || promotion.getStatus() != 1) return false;
        long threshold = promotion.getThresholdCent() == null ? 0 : promotion.getThresholdCent();
        return total >= threshold
                && (promotion.getBeginAt() == null || !now.isBefore(promotion.getBeginAt()))
                && (promotion.getEndAt() == null || !now.isAfter(promotion.getEndAt()));
    }

    private void publishLowStock(Long tenantId, Long productId, long available) {
        InventoryLowStockMessage msg = InventoryLowStockMessage.builder()
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .tenantId(tenantId)
                .productId(productId)
                .availableQuantity(available)
                .threshold((long) lowStockThreshold)
                .occurredAt(LocalDateTime.now())
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_INVENTORY_LOW_STOCK, msg);
        } catch (Exception e) {
            log.warn("发布低库存事件失败 productId={}", productId, e);
        }
    }

    @Override
    public OrderDTO get(Long orderId) {
        return toDTO(requireOrder(orderId));
    }

    @Override
    public PageResult<OrderDTO> page(int page, int size) {
        return page(page, size, null, null);
    }

    @Override
    public PageResult<OrderDTO> page(int page, int size, String status, String orderNo) {
        Long tenantId = TenantContext.getTenantId();
        QueryWrapper<Orders> query = new QueryWrapper<Orders>().eq("tenant_id", tenantId);
        if (status != null && !status.isBlank()) {
            query.eq("status", status);
        }
        if (orderNo != null && !orderNo.isBlank()) {
            query.like("order_no", orderNo.trim());
        }
        Page<Orders> p = ordersMapper.selectPage(new Page<>(page, size), query.orderByDesc("id"));
        return PageResult.of(p.getRecords().stream().map(this::toDTO).toList(),
                p.getTotal(), page, size);
    }

    @Override
    @Transactional
    public OrderDTO confirm(Long orderId) {
        return transit(orderId, OrderStatus.CONFIRMED, "CONFIRM_ORDER", false);
    }

    @Override
    @Transactional
    public OrderDTO ship(Long orderId) {
        return transit(orderId, OrderStatus.SHIPPED, "SHIP_ORDER", false);
    }

    @Override
    @Transactional
    public OrderDTO complete(Long orderId) {
        return transit(orderId, OrderStatus.COMPLETED, "COMPLETE_ORDER", false);
    }

    @Override
    @Transactional
    public OrderDTO cancel(Long orderId) {
        return transit(orderId, OrderStatus.CANCELLED, "CANCEL_ORDER", true);
    }

    @Override
    @Transactional
    public OrderDTO applyRefund(Long orderId) {
        return transit(orderId, OrderStatus.REFUNDING, "APPLY_REFUND", false);
    }

    @Override
    @Transactional
    public OrderDTO finishRefund(Long orderId) {
        return transit(orderId, OrderStatus.REFUNDED, "FINISH_REFUND", false);
    }

    @Override
    @Transactional
    public OrderDTO closeRefund(Long orderId) {
        return transit(orderId, OrderStatus.CANCELLED, "CLOSE_REFUND", false);
    }

    @Override
    public List<OrderDTO> listByCustomer(Long customerId) {
        // 顾客可以在商城内跨商家下单，订单归属商家租户，但“我的订单”必须按顾客身份聚合。
        List<Orders> list = ordersMapper.findByCustomerId(customerId);
        return list.stream().map(this::toDTO).toList();
    }

    @Override
    public List<OrderStatusHistoryDTO> getHistory(Long orderId) {
        requireOrder(orderId);
        List<OrderStatusHistory> list = historyMapper.selectList(
                new QueryWrapper<OrderStatusHistory>().eq("order_id", orderId).orderByAsc("id"));
        List<OrderStatusHistoryDTO> result = new ArrayList<>();
        for (OrderStatusHistory h : list) {
            OrderStatusHistoryDTO d = new OrderStatusHistoryDTO();
            d.setFromStatus(h.getFromStatus());
            d.setToStatus(h.getToStatus());
            d.setOperatorId(h.getOperatorId());
            d.setRemark(h.getRemark());
            d.setCreatedAt(h.getCreatedAt());
            result.add(d);
        }
        return result;
    }

    @Override
    public OrderStatsDTO stats() {
        Long tenantId = TenantContext.getTenantId();
        OrderStatsDTO dto = new OrderStatsDTO();
        List<Map<String, Object>> sc = ordersMapper.countByStatus(tenantId);
        List<OrderStatsDTO.StatusCount> dist = new ArrayList<>();
        long total = 0, pending = 0, completed = 0;
        for (Map<String, Object> m : sc) {
            String status = String.valueOf(m.get("status"));
            long cnt = ((Number) m.get("cnt")).longValue();
            dist.add(new OrderStatsDTO.StatusCount(status, cnt));
            total += cnt;
            if ("CREATED".equals(status)) pending = cnt;
            if ("COMPLETED".equals(status)) completed = cnt;
        }
        dto.setStatusDistribution(dist);
        dto.setTotalCount(total);
        dto.setPendingCount(pending);
        dto.setCompletedCount(completed);
        dto.setTodayCount(ordersMapper.countToday(tenantId));
        dto.setTotalSalesCent(ordersMapper.sumCompletedSales(tenantId));
        List<Map<String, Object>> ds = ordersMapper.dailyStats(tenantId, LocalDateTime.now().minusDays(7));
        List<OrderStatsDTO.DailyStat> days = new ArrayList<>();
        for (Map<String, Object> m : ds) {
            String date = String.valueOf(m.get("day"));
            long cnt = ((Number) m.get("cnt")).longValue();
            long amt = ((Number) m.get("amount")).longValue();
            days.add(new OrderStatsDTO.DailyStat(date, cnt, amt));
        }
        dto.setLast7Days(days);
        return dto;
    }

    private OrderDTO transit(Long orderId, OrderStatus target, String action, boolean releaseInventory) {
        Long tenantId = TenantContext.getTenantId();
        Orders order = requireOrder(orderId);
        OrderStatus current = OrderStatus.valueOf(order.getStatus());
        if (!OrderStatus.canTransition(current, target)) {
            throw new BizException(BizErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }

        if (releaseInventory) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new QueryWrapper<OrderItem>().eq("order_id", orderId));
            for (OrderItem oi : items) {
                inventoryMapper.release(tenantId, oi.getProductId(), oi.getQuantity());
            }
        }

        String from = order.getStatus();
        order.setStatus(target.name());
        ordersMapper.updateById(order);
        insertHistory(orderId, tenantId, from, target.name(), action);
        auditLogService.write(action, "order", String.valueOf(orderId), "from=" + from, "to=" + target.name());
        return toDTO(order);
    }

    private Orders requireOrder(Long orderId) {
        Long tenantId = TenantContext.getTenantId();
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !tenantId.equals(order.getTenantId())) {
            throw new BizException(BizErrorCode.ORDER_NOT_IN_TENANT);
        }
        return order;
    }

    private void insertHistory(Long orderId, Long tenantId, String from, String to, String remark) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setTenantId(tenantId);
        h.setOrderId(orderId);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setOperatorId(TenantContext.getUserId());
        h.setRemark(remark);
        historyMapper.insert(h);
    }

    private String generateOrderNo() {
        return "OF" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private OrderDTO toDTO(Orders order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerId(order.getCustomerId());
        dto.setStoreId(order.getStoreId());
        dto.setStoreName(order.getStoreNameSnapshot());
        dto.setStatus(order.getStatus());
        dto.setTotalAmountCent(order.getTotalAmountCent());
        dto.setPromoCode(order.getPromoCode());
        dto.setDiscountAmountCent(order.getDiscountAmountCent());
        dto.setCreatedAt(order.getCreatedAt());

        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
        dto.setItems(items.stream().map(oi -> {
            OrderDTO.OrderItemDTO d = new OrderDTO.OrderItemDTO();
            d.setProductId(oi.getProductId());
            d.setProductCode(oi.getProductCodeSnapshot());
            d.setProductName(oi.getProductNameSnapshot());
            d.setUnitPriceCent(oi.getUnitPriceCent());
            d.setQuantity(oi.getQuantity());
            d.setLineAmountCent(oi.getLineAmountCent());
            return d;
        }).toList());
        return dto;
    }
}
