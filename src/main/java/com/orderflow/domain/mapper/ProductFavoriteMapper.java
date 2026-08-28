package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.ProductFavorite;
import com.orderflow.favorite.FavoriteProductDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/* Mapper 只负责执行 sql 不编写用户权限等业务判断。
BaseMapper<ProductFavorite>额外提供 insert、selectById。deleteById 等基础 curd 方法
*/
@Mapper
public interface ProductFavoriteMapper extends BaseMapper<ProductFavorite> {
    /* 查询商品是否存在并上架，顾客能浏览不同商家的商品，所以跳过默认租户条件
    SQL 仍然会用 productId 和 status 明确限制了查询范围
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT COUNT(*)
            FROM product
            WHERE id= #{productId} AND status =1
            """
            )
    // @Param把 Java 参数命名为 productId，SQL 通过 #{productId} 安全绑定。
    long countActiveProduct(@Param("productId") Long productId);
    /*  把收藏数据写进数据库，利用唯一索引和 ON DUPLICATE KEY UPDATE实现幂等插入。
     */
    @Insert("""
        INSERT INTO product_favorite (tenant_id, customer_id, product_id, created_at)
        VALUES (#{favorite.tenantId}, #{favorite.customerId},
                #{favorite.productId}, #{favorite.createdAt})
        ON DUPLICATE KEY UPDATE id = id
        """)
        // 返回值是 SQL 影响行数，该业务只关系最终已收藏状态。
    int insertIdempotently(@Param("favorite") ProductFavorite favorite);
    /* 使用租户+顾客+商品三个条件删除，防止越权。
     * 返回1 代表真正删除了数据，0 表示本来就没有收藏。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Delete("""
            DELETE FROM product_favorite
            WHERE tenant_id =#{tenantId}
            AND customer_id =#{customerId}
            AND product_id =#{productId}
            """)
    int deleteByCustomerAndProduct(@Param("tenantId") Long tenantId,
                                   @Param("customerId") Long customerId,
                                   @Param("productId") Long productId);
    /* 查询当前顾客对某商品是否存在收藏记录 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
               SELECT COUNT(*)
               FROM product_favorite
               WHERE tenant_id =#{tenantId}
               AND customer_id =#{customerId}
               AND product_id =#{productId}
               """)
    long countByCustomerAndProduct(@Param("tenantId") Long tenantId,
                                   @Param("customerId") Long customerId,
                                   @Param("productId") Long productId);
/*计算当前顾客的有效收藏总数，用于前端分页组件
 *INNER JOIN中的 p.status=1 会过滤已下架或不存在的商品
 */
@InterceptorIgnore(tenantLine ="true")
@Select("""
        SELECT COUNT(*)
        FROM product_favorite f
        INNER JOIN product p ON p.id = f.product_id AND p.status =1
        WHERE f.tenant_id = #{tenantId}
        AND f.customer_id = #{customerId}
        """)
long countFavoriteProducts(@Param("tenantId") Long tenantId,
                           @Param("customerId") Long customerId);

/* 请求 join 一次查出收藏、商品、分类和店铺字段来避免每查到一条收藏就额外查一次商品的 n+1 问题。
 */
@InterceptorIgnore(tenantLine = "true")
@Select("""
        SELECT f.id AS favoriteId,
               f.product_id,
               p.product_code,
               p.product_name,
               p.unit_price_cent,
               p.category_id,
               c.category_name,
               p.store_id,
               s.store_name,
               COALESCE(p.sales,0) AS sales,
               f.created_at AS favorited_at
        FROM product_favorite f
        INNER JOIN product p ON p.id = f.product_id AND p.status =1
        LEFT JOIN category c ON c.id = p.category_id
        LEFT JOIN store s ON s.id = p.store_id
        WHERE f.tenant_id= #{tenantId}
        AND f.customer_id = #{customerId}

        ORDER BY f.created_at DESC
        LIMIT #{offset}, #{size}""")
    List<FavoriteProductDTO> selectFavoriteProducts(@Param("tenantId") Long tenantId,
                                                @Param("customerId") Long customerId,
                                                @Param("offset") long offset,
                                                @Param("size") long size);
}
