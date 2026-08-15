package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.OrderStatusHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderStatusHistoryMapper extends BaseMapper<OrderStatusHistory> {
}
