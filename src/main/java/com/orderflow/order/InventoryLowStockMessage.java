package com.orderflow.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 低库存告警事件消息（经由 RabbitMQ 传递的稳定数据契约）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLowStockMessage {
    private String eventId;
    private Long tenantId;
    private Long productId;
    private Long availableQuantity;
    private Long threshold;
    private LocalDateTime occurredAt;
}
