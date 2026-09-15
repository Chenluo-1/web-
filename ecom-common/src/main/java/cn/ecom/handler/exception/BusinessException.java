package cn.ecom.handler.exception;

import cn.ecom.utils.AppHttpCodeEnum;
import lombok.Getter;

/**
 * 业务异常：service 层对可预期的业务错误抛出，由全局异常处理器转成 ResponseResult
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(AppHttpCodeEnum httpCodeEnum) {
        super(httpCodeEnum.getMsg());
        this.code = httpCodeEnum.getCode();
    }

    public BusinessException(AppHttpCodeEnum httpCodeEnum, String message) {
        super(message);
        this.code = httpCodeEnum.getCode();
    }
}
