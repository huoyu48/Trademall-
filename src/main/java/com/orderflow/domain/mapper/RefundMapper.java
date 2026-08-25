package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.orderflow.domain.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM refund WHERE order_id = #{orderId} ORDER BY id DESC LIMIT 1")
    Refund findLatestByOrderId(@Param("orderId") Long orderId);

    /** 条件更新确保同一售后单的审核、收货、退款不会被重复执行。 */
    @Update("UPDATE refund SET status = #{targetStatus} WHERE id = #{id} AND tenant_id = #{tenantId} AND status = #{currentStatus}")
    int transitionStatus(@Param("id") Long id, @Param("tenantId") Long tenantId,
                         @Param("currentStatus") String currentStatus, @Param("targetStatus") String targetStatus);
}
