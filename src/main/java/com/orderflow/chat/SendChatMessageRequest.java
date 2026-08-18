package com.orderflow.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendChatMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息不能超过 1000 个字符")
    private String content;
}
