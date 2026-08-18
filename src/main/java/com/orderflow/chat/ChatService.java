package com.orderflow.chat;

import com.orderflow.security.LoginUser;

import java.util.List;

public interface ChatService {
    ChatConversationDTO openConversation(LoginUser customer, Long productId);

    List<ChatConversationDTO> listConversations(LoginUser user);

    ChatHistoryDTO history(LoginUser user, Long conversationId, Long beforeId, int size);

    ChatMessageDTO send(LoginUser user, Long conversationId, String content);

    void markRead(LoginUser user, Long conversationId);
}
