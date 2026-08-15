package com.orderflow.order;

import com.orderflow.config.RabbitMQConfig;
import com.orderflow.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 消费 order.created 事件：生成内部通知。
 * 若处理抛异常，监听容器会自动重试（最多 3 次），失败后路由到死信队列。
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final NotificationService notificationService;

    public OrderEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void onOrderCreated(OrderEventMessage message) {
        log.info("收到订单事件: orderNo={}, tenantId={}", message.getOrderNo(), message.getTenantId());
        notificationService.handleOrderCreated(message);
    }
}
