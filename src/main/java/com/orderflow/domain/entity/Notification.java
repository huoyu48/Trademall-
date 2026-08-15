package com.orderflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    /** 关联订单，非订单类事件（如库存告警）为 null */
    private Long orderId;
    /** 关联商品，非商品类事件为 null */
    private Long productId;
    private String eventType;
    private String channel;
    private String content;
    private Integer status;
    private LocalDateTime createdAt;
}
