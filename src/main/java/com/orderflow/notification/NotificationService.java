package com.orderflow.notification;

import com.orderflow.order.InventoryLowStockMessage;
import com.orderflow.order.OrderEventMessage;

public interface NotificationService {
    /**
     * 处理订单创建事件：落一条内部通知记录。
     * 真实场景此处会调用短信/邮件/站内信等渠道。
     */
    void handleOrderCreated(OrderEventMessage message);

    /**
     * 处理低库存告警事件：落一条内部告警记录，便于运营介入补货。
     */
    void handleLowStock(InventoryLowStockMessage message);
}
