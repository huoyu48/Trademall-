package com.orderflow.chat;

import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.domain.entity.ChatConversation;
import com.orderflow.domain.entity.ChatMessage;
import com.orderflow.domain.entity.Customer;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.mapper.ChatConversationMapper;
import com.orderflow.domain.mapper.ChatMessageMapper;
import com.orderflow.domain.mapper.CustomerMapper;
import com.orderflow.domain.mapper.ProductMapper;
import com.orderflow.domain.mapper.TenantMapper;
import com.orderflow.security.LoginUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 消息先落 MySQL，再在事务提交后推送 WebSocket。
 * WebSocket 不作为存储或权限判断依据，离线用户通过 REST 历史消息补拉。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final String CUSTOMER = "CUSTOMER";
    private static final String MERCHANT = "MERCHANT";
    private static final int MAX_HISTORY_SIZE = 100;

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final ProductMapper productMapper;
    private final TenantMapper tenantMapper;
    private final CustomerMapper customerMapper;
    private final ChatRealtimePublisher realtimePublisher;

    public ChatServiceImpl(ChatConversationMapper conversationMapper, ChatMessageMapper messageMapper,
                           ProductMapper productMapper, TenantMapper tenantMapper,
                           CustomerMapper customerMapper, ChatRealtimePublisher realtimePublisher) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.productMapper = productMapper;
        this.tenantMapper = tenantMapper;
        this.customerMapper = customerMapper;
        this.realtimePublisher = realtimePublisher;
    }

    @Override
    @Transactional
    public ChatConversationDTO openConversation(LoginUser customer, Long productId) {
        requireCustomer(customer);
        // 由启用商品反查商家，防止顾客构造任意 tenantId 开启会话。
        Long tenantId = productMapper.findActiveTenantId(productId);
        if (tenantId == null) {
            throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        }
        ChatConversation conversation = conversationMapper.findByCustomerAndTenant(customer.getUserId(), tenantId);
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setCustomerId(customer.getUserId());
            conversation.setTenantId(tenantId);
            conversation.setCustomerUnreadCount(0);
            conversation.setMerchantUnreadCount(0);
            try {
                conversationMapper.insertAny(conversation);
            } catch (DuplicateKeyException ignored) {
                // 同一顾客并发点击“联系商家”时，联合唯一索引收敛成同一会话。
                conversation = conversationMapper.findByCustomerAndTenant(customer.getUserId(), tenantId);
                if (conversation == null) throw ignored;
            }
        }
        return toConversationDTO(conversation, customer);
    }

    @Override
    public List<ChatConversationDTO> listConversations(LoginUser user) {
        if (isCustomer(user)) {
            return conversationMapper.findByCustomer(user.getUserId()).stream()
                    .map(c -> toConversationDTO(c, user)).toList();
        }
        requireMerchant(user);
        return conversationMapper.findByTenant(user.getTenantId()).stream()
                .map(c -> toConversationDTO(c, user)).toList();
    }

    @Override
    public ChatHistoryDTO history(LoginUser user, Long conversationId, Long beforeId, int size) {
        ChatConversation conversation = requireParticipant(user, conversationId);
        int safeSize = Math.max(1, Math.min(MAX_HISTORY_SIZE, size));
        List<ChatMessage> newestFirst = messageMapper.findHistory(conversation.getId(), beforeId, safeSize);
        Long nextBeforeId = newestFirst.size() == safeSize
                ? newestFirst.get(newestFirst.size() - 1).getId() : null;
        Collections.reverse(newestFirst);
        ChatHistoryDTO result = new ChatHistoryDTO();
        result.setList(newestFirst.stream().map(this::toMessageDTO).toList());
        result.setNextBeforeId(nextBeforeId);
        return result;
    }

    @Override
    @Transactional
    public ChatMessageDTO send(LoginUser user, Long conversationId, String content) {
        ChatConversation conversation = requireParticipant(user, conversationId);
        String normalized = content == null ? "" : content.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(40003, "消息内容不能为空");
        }

        boolean senderIsCustomer = isCustomer(user);
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setTenantId(conversation.getTenantId());
        message.setSenderType(senderIsCustomer ? CUSTOMER : MERCHANT);
        message.setSenderId(user.getUserId());
        message.setContent(normalized);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insertAny(message);
        conversationMapper.updateAfterMessage(conversation.getId(), preview(normalized), message.getCreatedAt(),
                senderIsCustomer ? 0 : 1, senderIsCustomer ? 1 : 0);

        ChatMessageDTO dto = toMessageDTO(message);
        // 只有提交成功后才广播；若事务回滚，双方不会看见一条不存在的消息。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtimePublisher.publish(conversation, dto);
            }
        });
        return dto;
    }

    @Override
    @Transactional
    public void markRead(LoginUser user, Long conversationId) {
        ChatConversation conversation = requireParticipant(user, conversationId);
        if (isCustomer(user)) {
            conversationMapper.clearCustomerUnread(conversation.getId());
            messageMapper.markReadBySenderType(conversation.getId(), MERCHANT);
        } else {
            conversationMapper.clearMerchantUnread(conversation.getId());
            messageMapper.markReadBySenderType(conversation.getId(), CUSTOMER);
        }
    }

    private ChatConversation requireParticipant(LoginUser user, Long conversationId) {
        ChatConversation conversation = conversationMapper.findAnyById(conversationId);
        if (conversation == null) throw new BizException(BizErrorCode.CHAT_CONVERSATION_NOT_FOUND);
        if (isCustomer(user) && conversation.getCustomerId().equals(user.getUserId())) return conversation;
        if (!isCustomer(user) && isMerchant(user) && conversation.getTenantId().equals(user.getTenantId())) return conversation;
        throw new BizException(BizErrorCode.CHAT_ACCESS_DENIED);
    }

    private ChatConversationDTO toConversationDTO(ChatConversation conversation, LoginUser viewer) {
        ChatConversationDTO dto = new ChatConversationDTO();
        dto.setId(conversation.getId());
        dto.setTenantId(conversation.getTenantId());
        dto.setLastMessageContent(conversation.getLastMessageContent());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        if (isCustomer(viewer)) {
            Tenant tenant = tenantMapper.selectById(conversation.getTenantId());
            dto.setPeerName(tenant == null ? "商家" : tenant.getTenantName());
            dto.setUnreadCount(conversation.getCustomerUnreadCount());
        } else {
            Customer customer = customerMapper.selectById(conversation.getCustomerId());
            dto.setPeerName(customer == null ? "顾客" : (StringUtils.hasText(customer.getNickname())
                    ? customer.getNickname() : customer.getUsername()));
            dto.setUnreadCount(conversation.getMerchantUnreadCount());
        }
        return dto;
    }

    private ChatMessageDTO toMessageDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderType(message.getSenderType());
        dto.setSenderId(message.getSenderId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setReadAt(message.getReadAt());
        return dto;
    }

    private String preview(String content) {
        return content.length() <= 200 ? content : content.substring(0, 200);
    }

    private boolean isCustomer(LoginUser user) {
        return user != null && user.getRoles() != null && user.getRoles().contains(CUSTOMER);
    }

    private boolean isMerchant(LoginUser user) {
        return user != null && user.getRoles() != null && user.getRoles().contains("MERCHANT_ADMIN");
    }

    private void requireCustomer(LoginUser user) {
        if (!isCustomer(user)) throw new BizException(BizErrorCode.CHAT_ACCESS_DENIED);
    }

    private void requireMerchant(LoginUser user) {
        if (!isMerchant(user)) throw new BizException(BizErrorCode.CHAT_ACCESS_DENIED);
    }
}
