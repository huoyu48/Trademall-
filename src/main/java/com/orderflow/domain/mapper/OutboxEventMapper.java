package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM outbox_event WHERE status = 'PENDING' AND next_retry_at <= NOW() ORDER BY id LIMIT #{limit}")
    List<OutboxEvent> findReady(@Param("limit") int limit);

    /** 抢占式状态转换，避免多实例定时任务重复投递同一事件。 */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE outbox_event SET status = 'SENDING' WHERE id = #{id} AND status = 'PENDING'")
    int claim(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE outbox_event SET status = 'SENT', sent_at = NOW(), last_error = NULL WHERE id = #{id} AND status = 'SENDING'")
    int markSent(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE outbox_event SET status = #{status}, retry_count = retry_count + 1, next_retry_at = #{nextRetryAt}, last_error = #{lastError} WHERE id = #{id} AND status = 'SENDING'")
    int markFailed(@Param("id") Long id, @Param("status") String status,
                   @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("lastError") String lastError);

    /** 进程在发送中崩溃时，将过期的租约归还给下一轮调度。 */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE outbox_event SET status = 'PENDING' WHERE status = 'SENDING' AND updated_at < #{cutoff}")
    int resetStaleSending(@Param("cutoff") LocalDateTime cutoff);
}
