package com.yezhen.hearbridge.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 *
 * 用于把业务异常转换成前端更容易理解的 JSON 响应，
 * 避免所有业务校验失败都变成 500 Internal Server Error。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数错误、业务校验错误。
     *
     * 例如：
     * 1. 分类编码为空
     * 2. 分类中文名为空
     * 3. 分类编码已存在
     *
     * @param exception 参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException exception) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", exception.getMessage());

        return body;
    }

    /**
     * 兜底处理未知异常。
     *
     * @param exception 未知异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "服务器内部错误");

        return body;
    }
}
