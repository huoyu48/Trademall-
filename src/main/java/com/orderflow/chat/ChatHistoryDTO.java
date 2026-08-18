package com.orderflow.chat;

import lombok.Data;

import java.util.List;

@Data
public class ChatHistoryDTO {
    private List<ChatMessageDTO> list;
    /** 下一页历史消息使用的 beforeId；为 null 表示没有更早记录。 */
    private Long nextBeforeId;
}
