package com.orderflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("refund")
public class Refund {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String refundNo;
    private Long orderId;
    private String orderNo;
    private String reason;
    private Long refundAmountCent;
    private String status;
    /** 退款驳回时用于把订单恢复到申请前状态。 */
    private String originalOrderStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
