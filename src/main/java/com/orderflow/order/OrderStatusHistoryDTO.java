package com.orderflow.order;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusHistoryDTO {
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
