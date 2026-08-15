package com.orderflow.order;

import com.orderflow.config.RabbitMQConfig;
import com.orderflow.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 消费 inventory.low-stock 事件：记录低库存告警，提示运营补货。
 * 若处理抛异常，监听容器会自动重试（最多 3 次），失败后路由到死信队列。
 */
@Component
public class InventoryLowStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryLowStockConsumer.class);

    private final NotificationService notificationService;

    public InventoryLowStockConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVENTORY_LOW_STOCK)
    public void onLowStock(InventoryLowStockMessage message) {
        log.warn("低库存告警：tenantId={}, productId={}, 可用量={}, 阈值={}",
                message.getTenantId(), message.getProductId(),
                message.getAvailableQuantity(), message.getThreshold());
        notificationService.handleLowStock(message);
    }
}
