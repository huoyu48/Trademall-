package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.NotificationFailure;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationFailureMapper extends BaseMapper<NotificationFailure> {

    /**
     * 死信记录属于跨租户的运维数据，且写入发生在 MQ 消费线程（无 TenantContext），
     * 因此显式跳过多租户拦截，避免"最后一道防线"自己抛 IllegalStateException。
     */
    @Override
    @InterceptorIgnore(tenantLine = "true")
    int insert(NotificationFailure entity);
}
