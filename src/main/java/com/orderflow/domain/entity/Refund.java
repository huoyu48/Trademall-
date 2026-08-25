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
    /** REFUND_ONLY=仅退款，RETURN_AND_REFUND=退货退款。 */
    private String refundType;
    private String status;
    private String returnLogisticsCompany;
    private String returnTrackingNo;
    private LocalDateTime returnShippedAt;
    private LocalDateTime merchantReceivedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
