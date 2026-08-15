package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE tenant_id = #{tenantId} AND product_code = #{productCode} LIMIT 1")
    Product findByCode(@Param("tenantId") Long tenantId, @Param("productCode") String productCode);
}
