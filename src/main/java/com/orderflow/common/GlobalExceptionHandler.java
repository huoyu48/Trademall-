package com.orderflow.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex) {
        log.warn("业务异常: code={}, msg={}", ex.getCode(), ex.getMessage());
        return ApiResponse.<Void>fail(ex.getCode(), ex.getMessage()).withRequestId(MDC.get("requestId"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.<Void>fail(ResultCode.VALIDATION_ERROR.getCode(), msg).withRequestId(MDC.get("requestId"));
    }

    /**
     * 路径不存在。若不单独处理会被下面的兜底分支吃掉，对外表现为 500，
     * 掩盖真实问题（前端拼错路径时会误以为是服务端故障）。
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("接口不存在: {} {}", request.getMethod(), request.getRequestURI());
        return ApiResponse.<Void>fail(ResultCode.NOT_FOUND.getCode(), "接口不存在")
                .withRequestId(MDC.get("requestId"));
    }

    /**
     * 请求方法不支持，同样不应归为 500。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                   HttpServletRequest request) {
        log.warn("请求方法不支持: {} {}", request.getMethod(), request.getRequestURI());
        return ApiResponse.<Void>fail(ResultCode.VALIDATION_ERROR.getCode(),
                "不支持的请求方法: " + request.getMethod()).withRequestId(MDC.get("requestId"));
    }

    /**
     * 方法级鉴权失败（@PreAuthorize）会抛 AuthorizationDeniedException（继承自 AccessDeniedException）。
     * 必须转成 403 而非兜底 500，否则越权访问会被误报成系统故障。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        log.warn("无权限访问: {}", ex.getMessage());
        return ApiResponse.<Void>fail(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage())
                .withRequestId(MDC.get("requestId"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleOther(Exception ex, HttpServletRequest request) {
        log.error("未捕获异常: {}", request.getRequestURI(), ex);
        return ApiResponse.<Void>fail(ResultCode.SYSTEM_ERROR.getCode(), "系统异常，请稍后重试")
                .withRequestId(MDC.get("requestId"));
    }
}
