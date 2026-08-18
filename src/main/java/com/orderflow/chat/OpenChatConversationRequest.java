package com.orderflow.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenChatConversationRequest {
    /** 由商品反查商家租户，顾客不能直接指定 tenantId。 */
    @NotNull(message = "商品不能为空")
    private Long productId;
}
