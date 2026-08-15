package com.orderflow.notification;

import com.orderflow.domain.entity.Notification;
import com.orderflow.domain.mapper.NotificationMapper;
import com.orderflow.order.InventoryLowStockMessage;
import com.orderflow.order.OrderEventMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional
    public void handleOrderCreated(OrderEventMessage message) {
        Notification n = new Notification();
        n.setTenantId(message.getTenantId());
        n.setOrderId(message.getOrderId());
        n.setEventType(message.getEventType());
        n.setChannel("INTERNAL");
        n.setContent("订单 " + message.getOrderNo() + " 已创建，等待处理");
        n.setStatus(1); // 已发送
        notificationMapper.insert(n);
    }

    @Override
    @Transactional
    public void handleLowStock(InventoryLowStockMessage message) {
        Notification n = new Notification();
        n.setTenantId(message.getTenantId());
        n.setOrderId(null); // 库存告警与具体订单无关
        n.setProductId(message.getProductId());
        n.setEventType("inventory.low-stock");
        n.setChannel("INTERNAL");
        n.setContent("商品 " + message.getProductId() + " 可用库存降至 " + message.getAvailableQuantity()
                + "（阈值 " + message.getThreshold() + "），请及时补货");
        n.setStatus(1);
        notificationMapper.insert(n);
    }
}
