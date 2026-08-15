package com.orderflow.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 经由 RabbitMQ 传递的内部事件消息（稳定的数据契约，不暴露完整 Entity）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventMessage {
    private String eventId;
    private String eventType;
    private Long orderId;
    private String orderNo;
    private Long tenantId;
    private LocalDateTime occurredAt;
    private String traceId;
    private List<OrderEventItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderEventItem {
        private Long productId;
        private Integer quantity;
    }
}
