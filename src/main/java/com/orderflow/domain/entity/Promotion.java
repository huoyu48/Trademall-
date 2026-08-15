package com.orderflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("promotion")
public class Promotion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String promoCode;
    private String promoName;
    private String promoType;
    private Long thresholdCent;
    private Long discountAmountCent;
    private LocalDateTime beginAt;
    private LocalDateTime endAt;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
