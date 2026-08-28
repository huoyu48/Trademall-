package com.orderflow.favorite;

import com.orderflow.common.PageResult;

/* 收藏业务接口 只定义收藏模块能做什么 不写具体逻辑。*/
public interface FavoriteService {
    // 收藏一个商品。
    FavoriteStatusDTO add(Long tenantId, Long customerId, Long productId);
    // 取消收藏，重复取消返回 false
    FavoriteStatusDTO remove(Long tenantId, Long customerId, Long productId);
    // 查询当前顾客是否已经收藏了该商品
    FavoriteStatusDTO status(Long tenantId, Long customerId, Long productId);
    // 分页查询当前顾客的收藏商品
    PageResult<FavoriteProductDTO> page(
        Long tenantId,
        Long customerId,
        int page,
        int size
    );
}
