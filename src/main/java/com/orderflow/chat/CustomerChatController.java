package com.orderflow.chat;

import com.orderflow.common.ApiResponse;
import com.orderflow.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 顾客侧：从商品详情进入咨询，查看自己的会话、历史和未读消息。 */
@RestController
@RequestMapping("/customer/chat")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerChatController {
    private final ChatService chatService;

    public CustomerChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations")
    public ApiResponse<ChatConversationDTO> open(@Valid @RequestBody OpenChatConversationRequest request) {
        return ApiResponse.success(chatService.openConversation(SecurityUtils.current(), request.getProductId()));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ChatConversationDTO>> list() {
        return ApiResponse.success(chatService.listConversations(SecurityUtils.current()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<ChatHistoryDTO> history(@PathVariable Long id,
                                                @RequestParam(required = false) Long beforeId,
                                                @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(chatService.history(SecurityUtils.current(), id, beforeId, size));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<ChatMessageDTO> send(@PathVariable Long id, @Valid @RequestBody SendChatMessageRequest request) {
        return ApiResponse.success(chatService.send(SecurityUtils.current(), id, request.getContent()));
    }

    @PostMapping("/conversations/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        chatService.markRead(SecurityUtils.current(), id);
        return ApiResponse.success();
    }
}
