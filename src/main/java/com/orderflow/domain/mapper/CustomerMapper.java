package com.orderflow.domain.mapper;

import com.orderflow.domain.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 顾客登录按用户名全局查找（customer 表已加入多租户忽略列表，避免登录时因无 tenant 上下文而注入 tenant_id 失败）。
     */
    @Select("SELECT * FROM customer WHERE username = #{username} LIMIT 1")
    Customer findByUsername(String username);
}
