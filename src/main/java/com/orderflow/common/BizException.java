package com.orderflow.common;

import lombok.Getter;

/**
 * 业务异常：携带错误码与消息，由全局异常处理器转换为统一响应。
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(BizErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public String getBizMessage() {
        return super.getMessage();
    }
}
