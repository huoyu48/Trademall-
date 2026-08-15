package com.orderflow.order;

import com.orderflow.common.BizException;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.mapper.OrdersMapper;
import com.orderflow.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消调度。
 * <p>
 * 周期性扫描仍处于 CREATED（已创建但未确认/支付）且超过 {@code timeout-minutes} 的订单，
 * 自动执行取消并释放其预占库存。这是电商“关单”能力的典型实现，
 * 既能避免库存被长期无效占用，也体现了状态机 + 定时任务 + 库存回补的组合运用。
 * <p>
 * 注意：调度线程没有 Web 请求的租户上下文，需在循环内手动设置并清理 TenantContext。
 */
@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    private final OrdersMapper ordersMapper;
    private final OrderService orderService;
    private final long timeoutMinutes;

    public OrderTimeoutScheduler(OrdersMapper ordersMapper, OrderService orderService,
                                 @Value("${orderflow.order.timeout-minutes:30}") long timeoutMinutes) {
        this.ordersMapper = ordersMapper;
        this.orderService = orderService;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(fixedDelay = 60000)
    public void cancelTimedOutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Orders> timedOut = ordersMapper.findTimedOut(cutoff);
        if (timedOut.isEmpty()) {
            return;
        }
        log.info("扫描到 {} 个超时未处理订单（超过 {} 分钟），开始自动取消", timedOut.size(), timeoutMinutes);
        for (Orders order : timedOut) {
            try {
                TenantContext.set(order.getTenantId(), order.getCreatedBy(), "scheduler");
                orderService.cancel(order.getId());
                log.info("订单 {} 已超时自动取消", order.getOrderNo());
            } catch (BizException e) {
                log.warn("订单 {} 自动取消被拒绝：{}", order.getOrderNo(), e.getMessage());
            } catch (Exception e) {
                log.error("订单 {} 自动取消异常", order.getOrderNo(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
