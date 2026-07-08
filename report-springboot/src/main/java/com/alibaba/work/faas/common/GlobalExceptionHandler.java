package com.alibaba.work.faas.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器 —— 兜住所有 Controller 没有 catch 的异常，
 * 统一返回 {@link ApiResponse} 格式，避免直接抛出 500 或堆栈泄露。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ========================================
    //  400 — 参数错误
    // ========================================

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.badRequest(e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResponse.badRequest("缺少必填参数: " + e.getParameterName());
    }

    // ========================================
    //  500 — 服务器内部错误
    // ========================================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Map<String, Object>> handleException(Exception e, HttpServletRequest request) {
        log.error("服务器内部错误: {} {}", request.getMethod(), request.getRequestURI(), e);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("path", request.getRequestURI());
        detail.put("method", request.getMethod());
        return ApiResponse.<Map<String, Object>>serverError("服务器内部错误")
                .withField("error", e.getClass().getSimpleName())
                .withField("detail", detail);
    }
}
