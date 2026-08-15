package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.Refund;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
}
