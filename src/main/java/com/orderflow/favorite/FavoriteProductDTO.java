package com.orderflow.favorite;

import lombok.Data;
import java.time.LocalDateTime;

/* 收藏页返回的展示数据。
组合了收藏、商品、分类和店铺的字段。
 */
@Data
public class FavoriteProductDTO {
    // 收藏记录本身的主键，前端可以把它作为列表唯一 key。
    private Long favoriteId;
    // 商品主键，用于调转商品详情或调用取消收藏接口。
    private Long productId;
    // 商品编码，例如 SKU-10001.
    private String productCode;
    // 商品单价，单位分。
    private Long unitPriceCent;
    // 商品分类主键。
    private Long categoryId;
    // 分类名称，由 category 表 join 得到。
    private String categoryName;
    // 商品所属店铺主键
    private Long storeId;
    // 店铺名称，由 store 表 join 得到。
    private String storeName;
    // 收藏时间，对应 product_favorite.created_at.
    private LocalDateTime favoritedAt;
    /** 商品名称。 */
    private String productName;

    /** 商品累计销量。 */
    private Long sales;
}