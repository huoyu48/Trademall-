package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.orderflow.domain.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE tenant_id = #{tenantId} AND product_code = #{productCode} LIMIT 1")
    Product findByCode(@Param("tenantId") Long tenantId, @Param("productCode") String productCode);

    /** 商城顾客从启用商品反查所属商家租户，不能相信前端传入的 tenantId。 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT tenant_id FROM product WHERE id = #{productId} AND status = 1 LIMIT 1")
    Long findActiveTenantId(@Param("productId") Long productId);
}
