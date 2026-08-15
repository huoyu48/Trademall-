package com.orderflow.common;

import lombok.Getter;

/**
 * 通用结果码。业务异常码见 {@link BizErrorCode}。
 */
@Getter
public enum ResultCode {
    SUCCESS(0, "成功"),
    SYSTEM_ERROR(50000, "系统异常"),
    VALIDATION_ERROR(40000, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或登录已失效"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
