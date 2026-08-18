package com.orderflow.common;

import lombok.Getter;

/**
 * 业务错误码。code 以 4/5 开头，便于与系统错误区分。
 */
@Getter
public enum BizErrorCode {
    LOGIN_FAILED(40101, "用户名或密码错误"),
    CUSTOMER_USERNAME_DUPLICATED(40909, "用户名已存在"),
    PASSWORD_CONFIRM_MISMATCH(40002, "两次输入的密码不一致"),
    TENANT_NOT_FOUND(40401, "租户不存在"),
    TENANT_CODE_DUPLICATED(40908, "租户编码已存在"),
    PRODUCT_CODE_DUPLICATED(40901, "商品编码已存在"),
    PRODUCT_NOT_FOUND(40402, "商品不存在"),
    PRODUCT_DISABLED(40902, "商品已停用"),
    PRODUCT_NOT_IN_TENANT(40301, "商品不属于当前租户"),
    INSUFFICIENT_INVENTORY(40903, "库存不足"),
    ORDER_NOT_FOUND(40403, "订单不存在"),
    ORDER_NOT_IN_TENANT(40302, "订单不属于当前租户"),
    INVALID_ORDER_STATUS_TRANSITION(40904, "非法的订单状态流转"),
    IDEMPOTENCY_CONFLICT(40905, "幂等键冲突，请使用返回的订单"),
    IDEMPOTENCY_KEY_REQUIRED(40001, "缺少 Idempotency-Key 请求头"),
    CANCEL_NOT_ALLOWED(40906, "当前订单状态不允许取消"),
    LOCK_ACQUIRE_FAILED(40907, "系统繁忙，请稍后重试"),
    CHAT_CONVERSATION_NOT_FOUND(40404, "会话不存在"),
    CHAT_ACCESS_DENIED(40303, "无权访问该会话"),
    NOT_FOUND(40400, "资源不存在");

    private final int code;
    private final String message;

    BizErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
