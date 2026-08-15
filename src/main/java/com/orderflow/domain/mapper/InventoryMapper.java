package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 预占库存：仅在可用量充足时成功（受影响行数=1 表示成功）。
     * 通过 `physical_quantity - reserved_quantity >= qty` 在数据库层保证并发安全，
     * 同时 version + 1 配合乐观锁语义。
     */
    @Update("""
            UPDATE inventory
            SET reserved_quantity = reserved_quantity + #{qty},
                version = version + 1
            WHERE tenant_id = #{tenantId}
              AND product_id = #{productId}
              AND physical_quantity - reserved_quantity >= #{qty}
            """)
    int reserve(@Param("tenantId") Long tenantId,
                @Param("productId") Long productId,
                @Param("qty") int qty);

    /**
     * 释放预占：取消订单时反向操作，仅在已预占量充足时成功。
     */
    @Update("""
            UPDATE inventory
            SET reserved_quantity = reserved_quantity - #{qty},
                version = version + 1
            WHERE tenant_id = #{tenantId}
              AND product_id = #{productId}
              AND reserved_quantity >= #{qty}
            """)
    int release(@Param("tenantId") Long tenantId,
                @Param("productId") Long productId,
                @Param("qty") int qty);

    @Select("SELECT * FROM inventory WHERE tenant_id = #{tenantId} AND product_id = #{productId} LIMIT 1")
    Inventory selectByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);
}
