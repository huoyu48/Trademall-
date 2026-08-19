package com.orderflow.order;

import com.orderflow.domain.entity.Promotion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPromotionSelectionTest {

    @Test
    void selectsTheLargestEligibleDiscountAndCalculatesRepeatedFullReduction() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 19, 12, 0);
        Promotion full200 = promotion("FULL200-30", 20_000L, 3_000L, now);
        Promotion full500 = promotion("FULL500-80", 50_000L, -8_000L, now);
        Promotion future = promotion("FUTURE", 0L, 10_000L, now.plusDays(1));

        Promotion selected = OrderServiceImpl.chooseBestPromotion(List.of(full200, full500, future), 349_900L, now);

        assertThat(selected).isSameAs(full500);
        assertThat(OrderServiceImpl.calculateDiscount(selected, 349_900L)).isEqualTo(48_000L);
    }

    @Test
    void ignoresPromotionsThatDoNotMeetTheThresholdOrAreDisabled() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 19, 12, 0);
        Promotion thresholdNotMet = promotion("FULL500-80", 50_000L, 8_000L, now);
        Promotion disabled = promotion("DISABLED", 0L, 10_000L, now);
        disabled.setStatus(0);

        assertThat(OrderServiceImpl.chooseBestPromotion(List.of(thresholdNotMet, disabled), 19_900L, now)).isNull();
    }

    private Promotion promotion(String code, long thresholdCent, long discountCent, LocalDateTime beginAt) {
        Promotion promotion = new Promotion();
        promotion.setPromoCode(code);
        promotion.setPromoType("FULL_REDUCTION");
        promotion.setThresholdCent(thresholdCent);
        promotion.setDiscountAmountCent(discountCent);
        promotion.setStatus(1);
        promotion.setBeginAt(beginAt.minusMinutes(1));
        promotion.setEndAt(beginAt.plusDays(1));
        return promotion;
    }
}
