package com.qinghe.mall.model;

public class Result<D> {

    private boolean success;
    private Integer code;
    private String message;
    private D data;

    public static <D> Result<D> success(D data) {
        Result<D> result = new Result<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <D> Result<D> success() {
        Result<D> result = new Result<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("success");
        return result;
    }

    public static <D> Result<D> fail(String message) {
        Result<D> result = new Result<>();
        result.setSuccess(false);
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <D> Result<D> fail(Integer code, String message) {
        Result<D> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }
}
