package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    @Select("SELECT * FROM orders WHERE tenant_id = #{tenantId} AND idempotency_key = #{key} LIMIT 1")
    Orders findByIdempotencyKey(@Param("tenantId") Long tenantId, @Param("key") String key);

    @Select("SELECT * FROM orders WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} LIMIT 1")
    Orders findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    /** 顾客订单跨商家展示，调用方必须已按 JWT customerId 完成身份校验。 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM orders WHERE customer_id = #{customerId} ORDER BY id DESC")
    List<Orders> findByCustomerId(@Param("customerId") Long customerId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM orders WHERE status IN ('PENDING_PAYMENT', 'CREATED') AND created_at < #{cutoff}")
    List<Orders> findTimedOut(@Param("cutoff") java.time.LocalDateTime cutoff);

    /** 条件更新保证同一订单的重复通知只会有一次真正的状态流转。 */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE orders SET status = 'PAID' WHERE id = #{orderId} AND customer_id = #{customerId} AND status = 'PENDING_PAYMENT'")
    int markPaid(@Param("orderId") Long orderId, @Param("customerId") Long customerId);

    @Update("UPDATE orders SET status = #{targetStatus} WHERE id = #{orderId} AND tenant_id = #{tenantId} " +
            "AND status = #{currentStatus}")
    int transitionStatus(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId,
                         @Param("currentStatus") String currentStatus, @Param("targetStatus") String targetStatus);

    @Select("SELECT status, COUNT(*) AS cnt FROM orders WHERE tenant_id = #{tenantId} GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM orders WHERE tenant_id = #{tenantId} AND DATE(created_at) = CURDATE() " +
            "AND status IN ('PAID', 'CONFIRMED', 'SHIPPED', 'COMPLETED')")
    long countTodayPaid(@Param("tenantId") Long tenantId);

    @Select("SELECT COALESCE(SUM(total_amount_cent), 0) FROM orders WHERE tenant_id = #{tenantId} " +
            "AND status IN ('PAID', 'CONFIRMED', 'SHIPPED', 'COMPLETED')")
    long sumCompletedSales(@Param("tenantId") Long tenantId);

    @Select("SELECT DATE(created_at) AS day, COUNT(*) AS cnt, COALESCE(SUM(total_amount_cent), 0) AS amount " +
            "FROM orders WHERE tenant_id = #{tenantId} AND created_at >= #{since} " +
            "AND status IN ('PAID', 'CONFIRMED', 'SHIPPED', 'COMPLETED') " +
            "GROUP BY DATE(created_at) ORDER BY day")
    List<Map<String, Object>> dailyStats(@Param("tenantId") Long tenantId, @Param("since") java.time.LocalDateTime since);
}
