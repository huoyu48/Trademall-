package com.orderflow.favorite;

public record FavoriteStatusDTO (Long productId,boolean favorited){
    /* 收藏或取消收藏后的最新状态。
    @param productId 本次查询或操作的商品主键
    @param favorited true 表示已收藏 反之则为未收藏
     */

}
