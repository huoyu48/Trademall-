package com.orderflow.chat;

import com.orderflow.security.JwtTokenProvider;
import com.orderflow.security.LoginUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * 浏览器 WebSocket 握手无法附加 Authorization 请求头，故端点本身放行；
 * 真正的 JWT 鉴权在 STOMP CONNECT 帧完成，未认证连接无法订阅用户队列。
 */
@Component
public class ChatStompAuthInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    public ChatStompAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("缺少 WebSocket 身份凭证");
            }
            String token = header.substring(7);
            if (!jwtTokenProvider.validate(token)) {
                throw new MessagingException("WebSocket 登录已失效");
            }
            LoginUser user = jwtTokenProvider.parseToken(token);
            accessor.setUser(new ChatPrincipal(user));
        }
        return message;
    }
}
