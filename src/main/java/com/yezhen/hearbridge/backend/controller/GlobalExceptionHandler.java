package com.yezhen.hearbridge.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 全局异常处理器。
 *
 * 用于把业务异常转换成前端更容易理解的 JSON 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数错误、业务校验错误。
     *
     * @param exception 参数异常
     * @param request   当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return buildBody(400, "Bad Request", exception.getMessage(), request.getRequestURI());
    }

    /**
     * 处理带 HTTP 状态码的异常。
     *
     * 例如：
     * 1. 上传文件为空；
     * 2. 上传文件类型不支持；
     * 3. 上传文件过大；
     * 4. MinIO 上传失败。
     *
     * @param exception 带状态码的异常
     * @param request   当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {

        int statusCode = exception.getStatusCode().value();
        String reason = exception.getReason() == null ? "请求处理失败" : exception.getReason();

        String message = reason;
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            message = reason + "：" + exception.getCause().getMessage();
        }

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(buildBody(statusCode, exception.getStatusCode().toString(), message, request.getRequestURI()));
    }


    /**
     * 处理缺少请求参数 / multipart 文件 part 的异常。
     *
     * 例如：
     * 1. POST /files/upload 缺少 file；
     * 2. POST /files/upload 缺少 bizType。
     *
     * @param exception 参数异常
     * @param request   当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingRequestPartOrParameter(
            Exception exception,
            HttpServletRequest request) {
        return buildBody(
                400,
                "Bad Request",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 处理上传文件超过 Spring multipart 限制的异常。
     *
     * @param exception 文件过大异常
     * @param request   当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        return buildBody(
                400,
                "Bad Request",
                "上传文件大小超过限制",
                request.getRequestURI()
        );
    }

    /**
     * 兜底处理未知异常。
     *
     * 开发阶段打印异常堆栈，方便排查。
     * 后续正式部署时，可以把 message 改回“服务器内部错误”。
     *
     * @param exception 未知异常
     * @param request   当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception exception, HttpServletRequest request) {
        exception.printStackTrace();

        return buildBody(
                500,
                "Internal Server Error",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 构造统一错误响应体。
     *
     * @param status  HTTP 状态码
     * @param error   错误类型
     * @param message 错误信息
     * @param path    请求路径
     * @return 错误响应体
     */
    private Map<String, Object> buildBody(int status, String error, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);

        return body;
    }
}
