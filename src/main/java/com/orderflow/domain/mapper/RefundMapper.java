package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.orderflow.domain.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    /** 顾客售后列表跨商家展示；只通过订单的 customer_id 关联，避免按前端传入租户查询。 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT r.* FROM refund r JOIN orders o ON o.id = r.order_id " +
            "WHERE o.customer_id = #{customerId} ORDER BY r.id DESC")
    List<Refund> findByCustomerId(@Param("customerId") Long customerId);
}
