package com.orderflow.promotion;

import lombok.Data;

import java.time.LocalDateTime;

@Data
class CreatePromotionRequest {
    private String promoCode;
    private String promoName;
    private String promoType;
    private Long thresholdCent;
    private Long discountAmountCent;
    private LocalDateTime beginAt;
    private LocalDateTime endAt;
    private Integer status;
}

@Data
class UpdatePromotionRequest {
    private String promoName;
    private String promoType;
    private Long thresholdCent;
    private Long discountAmountCent;
    private LocalDateTime beginAt;
    private LocalDateTime endAt;
    private Integer status;
}
