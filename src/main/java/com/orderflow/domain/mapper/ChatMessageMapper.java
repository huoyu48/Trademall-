package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO chat_message (conversation_id, tenant_id, sender_type, sender_id, content, created_at) "
            + "VALUES (#{conversationId}, #{tenantId}, #{senderType}, #{senderId}, #{content}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAny(ChatMessage message);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} "
            + "AND (#{beforeId} IS NULL OR id < #{beforeId}) ORDER BY id DESC LIMIT #{size}")
    List<ChatMessage> findHistory(@Param("conversationId") Long conversationId,
                                  @Param("beforeId") Long beforeId, @Param("size") int size);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE chat_message SET read_at = NOW() WHERE conversation_id = #{conversationId} "
            + "AND sender_type = #{senderType} AND read_at IS NULL")
    int markReadBySenderType(@Param("conversationId") Long conversationId, @Param("senderType") String senderType);
}
