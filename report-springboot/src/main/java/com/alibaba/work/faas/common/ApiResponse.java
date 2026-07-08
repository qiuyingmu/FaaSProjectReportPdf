package com.alibaba.work.faas.common;

import java.util.LinkedHashMap;

/**
 * 统一 API 响应体 —— 所有 Controller 返回值类型。
 *
 * <p>确保前端收到的每个响应格式一致：</p>
 * <pre>{@code
 * {
 *   "success": true|false,
 *   "data": { ... },
 *   "message": "操作成功",
 *   "code": 200
 * }
 * }</pre>
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
public class ApiResponse<T> extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    // ========================================
    //  静态工厂方法
    // ========================================

    /** 成功（带数据） */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>().withSuccess(true).withCode(200).withData(data).withMessage("操作成功");
    }

    /** 成功（仅消息） */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /** 失败 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<T>().withSuccess(false).withCode(code).withMessage(message);
    }

    /** 参数校验失败（400） */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(400, message);
    }

    /** 未授权（401） */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(401, message);
    }

    /** 服务器内部错误（500） */
    public static <T> ApiResponse<T> serverError(String message) {
        return error(500, message);
    }

    // ========================================
    //  Fluent setter
    // ========================================

    @SuppressWarnings("unchecked")
    public ApiResponse<T> withSuccess(boolean success) {
        put("success", success);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ApiResponse<T> withCode(int code) {
        put("code", code);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ApiResponse<T> withMessage(String message) {
        put("message", message);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ApiResponse<T> withData(T data) {
        put("data", data);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ApiResponse<T> withField(String key, Object value) {
        put(key, value);
        return this;
    }
}
