package com.orderflow.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应封装。成功与失败共用，data 在失败时可为 null。
 */
@Data
public class ApiResponse<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private String requestId;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> fail(ResultCode rc) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(rc.getCode());
        r.setMessage(rc.getMessage());
        return r;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public ApiResponse<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
