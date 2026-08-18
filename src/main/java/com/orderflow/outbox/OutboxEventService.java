package com.orderflow.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.config.RabbitMQConfig;
import com.orderflow.domain.entity.OutboxEvent;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.mapper.OutboxEventMapper;
import com.orderflow.order.OrderEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 将业务事件与订单事务一同落库，并以至少一次语义异步投递消息队列。 */
@Service
public class OutboxEventService {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventService.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;

    public OutboxEventService(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper,
                              RabbitTemplate rabbitTemplate,
                              @Value("${orderflow.outbox.batch-size:100}") int batchSize) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
    }

    /** 由创建订单的同一数据库事务调用；未提交的订单绝不会产生可投递事件。 */
    public void recordOrderCreated(Orders order, List<OrderEventMessage.OrderEventItem> items, String traceId) {
        String eventId = UUID.randomUUID().toString().replace("-", "");
        OrderEventMessage message = OrderEventMessage.builder()
                .eventId(eventId)
                .eventType("order.created")
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .tenantId(order.getTenantId())
                .occurredAt(LocalDateTime.now())
                .traceId(traceId)
                .items(items)
                .build();
        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setTenantId(order.getTenantId());
        event.setAggregateType("order");
        event.setAggregateId(order.getId());
        event.setEventType(message.getEventType());
        event.setPayload(write(message));
        event.setStatus("PENDING");
        event.setRetryCount(0);
        event.setNextRetryAt(LocalDateTime.now());
        outboxEventMapper.insert(event);
    }

    @Scheduled(fixedDelayString = "${orderflow.outbox.publish-interval-ms:3000}")
    public void publishPending() {
        outboxEventMapper.resetStaleSending(LocalDateTime.now().minusMinutes(5));
        for (OutboxEvent event : outboxEventMapper.findReady(batchSize)) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        if (outboxEventMapper.claim(event.getId()) == 0) {
            return;
        }
        try {
            OrderEventMessage message = objectMapper.readValue(event.getPayload(), OrderEventMessage.class);
            CorrelationData correlation = new CorrelationData(event.getEventId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_ORDER_CREATED,
                    message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ 未确认: " + confirm.getReason());
            }
            outboxEventMapper.markSent(event.getId());
        } catch (Exception ex) {
            int nextRetry = event.getRetryCount() + 1;
            String status = nextRetry >= MAX_RETRIES ? "FAILED" : "PENDING";
            long delaySeconds = Math.min(60L, 2L << Math.min(event.getRetryCount(), 5));
            outboxEventMapper.markFailed(event.getId(), status,
                    LocalDateTime.now().plusSeconds(delaySeconds), truncate(ex.getMessage()));
            log.warn("Outbox 事件投递失败: eventId={}, retry={}, status={}",
                    event.getEventId(), nextRetry, status, ex);
        }
    }

    private String write(OrderEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("订单事件序列化失败", ex);
        }
    }

    private String truncate(String message) {
        if (message == null) return "unknown error";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
