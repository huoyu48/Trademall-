package com.orderflow.order;

import com.orderflow.config.RabbitMQConfig;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 在订单事务提交后，才将消息发送到 RabbitMQ。
 * 这是文档 11.2 描述的“学习版可靠发布”：若进程在提交后、发送前崩溃仍可能漏发，
 * 生产环境应改为 Outbox 表 + 可靠投递。此处如实保留该边界。
 */
@Component
public class OrderEventAfterCommitListener {

    private final RabbitTemplate rabbitTemplate;

    public OrderEventAfterCommitListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        OrderEventMessage message = OrderEventMessage.builder()
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .eventType("order.created")
                .orderId(event.getOrderId())
                .orderNo(event.getOrderNo())
                .tenantId(event.getTenantId())
                .occurredAt(LocalDateTime.now())
                .traceId(MDC.get("requestId"))
                .items(event.getItems())
                .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_ORDER_CREATED, message);
    }
}
