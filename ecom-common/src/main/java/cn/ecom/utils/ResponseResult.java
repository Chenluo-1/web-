package cn.ecom.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体：所有接口的返回类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;

    public ResponseResult(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static <T> ResponseResult<T> okResult() {
        return new ResponseResult<>(AppHttpCodeEnum.SUCCESS.getCode(), AppHttpCodeEnum.SUCCESS.getMsg());
    }

    public static <T> ResponseResult<T> okResult(T data) {
        ResponseResult<T> result = okResult();
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> errorResult(int code, String msg) {
        return new ResponseResult<>(code, msg);
    }

    public static <T> ResponseResult<T> errorResult(AppHttpCodeEnum httpCodeEnum) {
        return new ResponseResult<>(httpCodeEnum.getCode(), httpCodeEnum.getMsg());
    }

    public static <T> ResponseResult<T> errorResult(AppHttpCodeEnum httpCodeEnum, String msg) {
        return new ResponseResult<>(httpCodeEnum.getCode(), msg);
    }
}
