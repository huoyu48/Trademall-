package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    @Select("SELECT * FROM orders WHERE tenant_id = #{tenantId} AND idempotency_key = #{key} LIMIT 1")
    Orders findByIdempotencyKey(@Param("tenantId") Long tenantId, @Param("key") String key);

    @Select("SELECT * FROM orders WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} LIMIT 1")
    Orders findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM orders WHERE status = 'CREATED' AND created_at < #{cutoff}")
    List<Orders> findTimedOut(@Param("cutoff") java.time.LocalDateTime cutoff);

    @Select("SELECT status, COUNT(*) AS cnt FROM orders WHERE tenant_id = #{tenantId} GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM orders WHERE tenant_id = #{tenantId} AND DATE(created_at) = CURDATE()")
    long countToday(@Param("tenantId") Long tenantId);

    @Select("SELECT COALESCE(SUM(total_amount_cent), 0) FROM orders WHERE tenant_id = #{tenantId} AND status = 'COMPLETED'")
    long sumCompletedSales(@Param("tenantId") Long tenantId);

    @Select("SELECT DATE(created_at) AS day, COUNT(*) AS cnt, COALESCE(SUM(total_amount_cent), 0) AS amount " +
            "FROM orders WHERE tenant_id = #{tenantId} AND created_at >= #{since} " +
            "GROUP BY DATE(created_at) ORDER BY day")
    List<Map<String, Object>> dailyStats(@Param("tenantId") Long tenantId, @Param("since") java.time.LocalDateTime since);
}
