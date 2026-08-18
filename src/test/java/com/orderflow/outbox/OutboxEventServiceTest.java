package com.orderflow.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderflow.domain.entity.OutboxEvent;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.mapper.OutboxEventMapper;
import com.orderflow.order.OrderEventMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {
    @Mock private OutboxEventMapper outboxEventMapper;
    @Mock private RabbitTemplate rabbitTemplate;

    @Test
    void recordsOrderEventBeforeItIsPublished() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OutboxEventService service = new OutboxEventService(outboxEventMapper, objectMapper, rabbitTemplate, 100);
        Orders order = new Orders();
        order.setId(8L);
        order.setTenantId(3L);
        order.setOrderNo("OF-8");

        service.recordOrderCreated(order, List.of(OrderEventMessage.OrderEventItem.builder()
                .productId(21L).quantity(2).build()), "trace-1");

        ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(event.capture());
        assertEquals("PENDING", event.getValue().getStatus());
        assertEquals(3L, event.getValue().getTenantId());
        OrderEventMessage payload = objectMapper.readValue(event.getValue().getPayload(), OrderEventMessage.class);
        assertEquals(event.getValue().getEventId(), payload.getEventId());
        assertEquals(8L, payload.getOrderId());
        assertNotNull(payload.getOccurredAt());
    }
}
