package com.orderflow.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.config.RabbitMQConfig;
import com.orderflow.domain.entity.NotificationFailure;
import com.orderflow.domain.mapper.NotificationFailureMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 死信队列消费者：记录最终失败的事件，便于排查与人工重投。
 *
 * <p>设计约束：这是可靠性链路的最后一道防线，<b>自身绝不能再失败</b>。因此：</p>
 * <ul>
 *   <li>参数用原始 {@link Message} 而非具体消息类型——DLQ 会汇聚多种事件
 *       （order.created / inventory.low-stock ...），绑定单一类型会让结构不匹配的消息
 *       反序列化出全 null 字段，进而插入失败、再次死信循环；</li>
 *   <li>字段尽力解析（best-effort），解析不到就落占位值，同时把原始 payload 存进 error_msg；</li>
 *   <li>全程 try-catch，任何异常只记日志、不外抛，保证消息被 ack 掉。</li>
 * </ul>
 */
@Component
public class OrderDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderDeadLetterConsumer.class);
    private static final int MAX_ERROR_MSG = 900;

    private final NotificationFailureMapper failureMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderDeadLetterConsumer(NotificationFailureMapper failureMapper) {
        this.failureMapper = failureMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ, containerFactory = "dlqListenerContainerFactory")
    public void onDeadLetter(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String origin = originQueue(message);
        try {
            JsonNode node = objectMapper.readTree(payload);
            NotificationFailure failure = new NotificationFailure();
            // tenant_id 为 NOT NULL，解析不到时用 0 占位，表示"归属未知"，仍保留原文可追溯
            failure.setTenantId(node.path("tenantId").isNumber() ? node.path("tenantId").asLong() : 0L);
            failure.setEventId(text(node, "eventId", "unknown-" + message.getMessageProperties().getMessageId()));
            failure.setEventType(text(node, "eventType", origin));
            failure.setOrderId(node.path("orderId").isNumber() ? node.path("orderId").asLong() : null);
            failure.setErrorMsg(truncate("消费重试耗尽，来源队列=" + origin + "，原始消息=" + payload));
            failure.setRetryCount(RabbitMQConfig.MAX_CONSUME_ATTEMPTS);
            failure.setStatus(2); // 已丢弃，等待人工重投
            failureMapper.insert(failure);
            log.error("事件进入死信队列并已归档: originQueue={}, eventId={}, payload={}",
                    origin, failure.getEventId(), payload);
        } catch (Exception ex) {
            // 连归档都失败时只能记日志，绝不能把异常抛回容器造成死信循环
            log.error("死信归档失败（消息已丢弃）: originQueue={}, payload={}, err={}",
                    origin, payload, ex.getMessage(), ex);
        }
    }

    /** 从 RabbitMQ 的 x-death 头里取原始队列名，便于定位是哪条链路失败。 */
    @SuppressWarnings("unchecked")
    private String originQueue(Message message) {
        try {
            Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
            if (xDeath instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> first) {
                Object queue = ((Map<String, Object>) first).get("queue");
                if (queue != null) {
                    return String.valueOf(queue);
                }
            }
        } catch (Exception ignored) {
            // 头信息缺失不影响归档
        }
        return "unknown";
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode v = node.path(field);
        return v.isTextual() && !v.asText().isBlank() ? v.asText() : fallback;
    }

    private String truncate(String s) {
        return s.length() <= MAX_ERROR_MSG ? s : s.substring(0, MAX_ERROR_MSG) + "...";
    }
}
