package com.orderflow.order;

import com.orderflow.domain.entity.NotificationFailure;
import com.orderflow.domain.mapper.NotificationFailureMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderDeadLetterConsumerTest {
    @Mock private NotificationFailureMapper failureMapper;

    @Test
    void archivesDeadLetterWithoutNeedingTenantContext() {
        OrderDeadLetterConsumer consumer = new OrderDeadLetterConsumer(failureMapper);
        String body = "{\"eventId\":\"event-1\",\"eventType\":\"order.created\",\"tenantId\":7,\"orderId\":9}";
        consumer.onDeadLetter(new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties()));

        ArgumentCaptor<NotificationFailure> failure = ArgumentCaptor.forClass(NotificationFailure.class);
        verify(failureMapper).insert(failure.capture());
        assertEquals("event-1", failure.getValue().getEventId());
        assertEquals(7L, failure.getValue().getTenantId());
        assertEquals(9L, failure.getValue().getOrderId());
    }
}
