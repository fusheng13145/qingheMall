package com.qinghe.mall.common;

import java.io.Serializable;

/**
 * 通用响应格式
 */
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> success() {
        return new R<>(StatusCode.SUCCESS, "操作成功", null);
    }

    public static <T> R<T> success(T data) {
        return new R<>(StatusCode.SUCCESS, "操作成功", data);
    }

    public static <T> R<T> success(String message, T data) {
        return new R<>(StatusCode.SUCCESS, message, data);
    }

    public static <T> R<T> error(String message) {
        return new R<>(StatusCode.SERVER_ERROR, message, null);
    }

    public static <T> R<T> error(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> error(ResponseCode responseCode) {
        return new R<>(responseCode.getCode(), responseCode.getMessage(), null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "R{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
