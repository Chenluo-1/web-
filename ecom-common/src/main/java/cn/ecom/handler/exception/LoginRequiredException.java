package cn.ecom.handler.exception;

/**
 * 未登录异常：权限校验时无登录态抛出，由全局异常处理器转成 401 响应
 */
public class LoginRequiredException extends RuntimeException {

    public LoginRequiredException(String message) {
        super(message);
    }
}
