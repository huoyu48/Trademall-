package com.orderflow.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 应用内本地事件：订单创建成功后由 Service 发布，
 * 由 {@code OrderEventAfterCommitListener} 在事务提交后转发到 RabbitMQ。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String orderNo;
    private Long tenantId;
    private List<OrderEventMessage.OrderEventItem> items;
}
