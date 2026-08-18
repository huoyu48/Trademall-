package com.orderflow.chat;

import com.orderflow.security.LoginUser;

import java.security.Principal;

/** STOMP 用户目的地使用角色加用户 ID，避免顾客与商家存在同号 ID 时串消息。 */
public class ChatPrincipal implements Principal {
    private final String name;

    public ChatPrincipal(LoginUser user) {
        String kind = user.getRoles() != null && user.getRoles().contains("CUSTOMER") ? "CUSTOMER" : "MERCHANT";
        this.name = kind + ":" + user.getUserId();
    }

    @Override
    public String getName() {
        return name;
    }
}
