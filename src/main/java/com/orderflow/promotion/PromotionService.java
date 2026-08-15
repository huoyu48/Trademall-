package com.orderflow.promotion;

import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Promotion;

import java.util.List;

public interface PromotionService {
    Promotion create(CreatePromotionRequest request);

    Promotion update(Long id, UpdatePromotionRequest request);

    Promotion detail(Long id);

    PageResult<Promotion> page(int page, int size);

    List<Promotion> listAll();
}
