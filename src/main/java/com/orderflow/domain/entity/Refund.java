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
    /** REFUND_ONLY：仅退款；RETURN_REFUND：退货退款。 */
    private String afterSaleType;
    /** 顾客寄回商品后填写的物流单号。 */
    private String returnTrackingNo;
    private LocalDateTime returnApprovedAt;
    private LocalDateTime returnedAt;
    private LocalDateTime receivedAt;
    /** 退款驳回时用于把订单恢复到申请前状态。 */
    private String originalOrderStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
