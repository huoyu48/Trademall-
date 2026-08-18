package com.orderflow.chat;

import com.orderflow.domain.entity.ChatConversation;
import com.orderflow.domain.entity.ChatMessage;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.mapper.ChatConversationMapper;
import com.orderflow.domain.mapper.ChatMessageMapper;
import com.orderflow.domain.mapper.CustomerMapper;
import com.orderflow.domain.mapper.ProductMapper;
import com.orderflow.domain.mapper.TenantMapper;
import com.orderflow.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {
    private final ChatConversationMapper conversationMapper = mock(ChatConversationMapper.class);
    private final ChatMessageMapper messageMapper = mock(ChatMessageMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final TenantMapper tenantMapper = mock(TenantMapper.class);
    private final CustomerMapper customerMapper = mock(CustomerMapper.class);
    private final ChatRealtimePublisher realtimePublisher = mock(ChatRealtimePublisher.class);
    private final ChatServiceImpl service = new ChatServiceImpl(conversationMapper, messageMapper, productMapper,
            tenantMapper, customerMapper, realtimePublisher);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void opensOneConversationForCustomerAndMerchant() {
        LoginUser customer = customer(10L);
        Tenant tenant = new Tenant();
        tenant.setTenantName("数码商家");
        when(productMapper.findActiveTenantId(99L)).thenReturn(2L);
        when(conversationMapper.findByCustomerAndTenant(10L, 2L)).thenReturn(null);
        when(tenantMapper.selectById(2L)).thenReturn(tenant);
        org.mockito.Mockito.doAnswer(invocation -> {
            ChatConversation c = invocation.getArgument(0);
            c.setId(7L);
            return 1;
        }).when(conversationMapper).insertAny(any(ChatConversation.class));

        ChatConversationDTO result = service.openConversation(customer, 99L);

        assertEquals(7L, result.getId());
        assertEquals("数码商家", result.getPeerName());
        assertEquals(0, result.getUnreadCount());
        verify(conversationMapper).insertAny(any(ChatConversation.class));
    }

    @Test
    void persistsThenPublishesOnlyAfterCommit() {
        LoginUser customer = customer(10L);
        ChatConversation conversation = new ChatConversation();
        conversation.setId(7L);
        conversation.setTenantId(2L);
        conversation.setCustomerId(10L);
        when(conversationMapper.findAnyById(7L)).thenReturn(conversation);
        org.mockito.Mockito.doAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(88L);
            return 1;
        }).when(messageMapper).insertAny(any(ChatMessage.class));
        TransactionSynchronizationManager.initSynchronization();

        ChatMessageDTO result = service.send(customer, 7L, "  你好，想咨询库存  ");

        assertEquals(88L, result.getId());
        assertEquals("你好，想咨询库存", result.getContent());
        verify(conversationMapper).updateAfterMessage(eq(7L), eq("你好，想咨询库存"), any(), eq(0), eq(1));
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        ArgumentCaptor<ChatMessageDTO> captor = ArgumentCaptor.forClass(ChatMessageDTO.class);
        verify(realtimePublisher).publish(eq(conversation), captor.capture());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    private LoginUser customer(Long id) {
        return LoginUser.builder().userId(id).tenantId(1L).username("customer")
                .roles(List.of("CUSTOMER")).build();
    }
}
