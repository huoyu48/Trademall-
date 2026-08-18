package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.ChatConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO chat_conversation (tenant_id, customer_id, customer_unread_count, merchant_unread_count) "
            + "VALUES (#{tenantId}, #{customerId}, 0, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAny(ChatConversation conversation);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM chat_conversation WHERE customer_id = #{customerId} AND tenant_id = #{tenantId} LIMIT 1")
    ChatConversation findByCustomerAndTenant(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM chat_conversation WHERE customer_id = #{customerId} ORDER BY last_message_at DESC, id DESC")
    List<ChatConversation> findByCustomer(@Param("customerId") Long customerId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM chat_conversation WHERE tenant_id = #{tenantId} ORDER BY last_message_at DESC, id DESC")
    List<ChatConversation> findByTenant(@Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM chat_conversation WHERE id = #{id} LIMIT 1")
    ChatConversation findAnyById(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE chat_conversation SET last_message_content = #{content}, last_message_at = #{at}, "
            + "customer_unread_count = customer_unread_count + #{customerDelta}, "
            + "merchant_unread_count = merchant_unread_count + #{merchantDelta} WHERE id = #{id}")
    int updateAfterMessage(@Param("id") Long id, @Param("content") String content,
                           @Param("at") LocalDateTime at, @Param("customerDelta") int customerDelta,
                           @Param("merchantDelta") int merchantDelta);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE chat_conversation SET customer_unread_count = 0 WHERE id = #{id}")
    int clearCustomerUnread(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE chat_conversation SET merchant_unread_count = 0 WHERE id = #{id}")
    int clearMerchantUnread(@Param("id") Long id);
}
