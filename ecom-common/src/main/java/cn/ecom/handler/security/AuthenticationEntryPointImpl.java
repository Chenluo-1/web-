package cn.ecom.handler.security;

import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.WebUtils;
import com.alibaba.fastjson.JSON;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 未认证处理器：未登录访问受保护接口 → HTTP 401 + 统一响应体
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        ResponseResult<Void> result;
        if (authException instanceof BadCredentialsException) {
            result = ResponseResult.errorResult(AppHttpCodeEnum.USERNAME_OR_PASSWORD_ERROR, authException.getMessage());
        } else if (authException instanceof InsufficientAuthenticationException) {
            result = ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, "请先登录");
        } else {
            result = ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, "认证失败");
        }
        WebUtils.renderString(response, JSON.toJSONString(result), HttpServletResponse.SC_UNAUTHORIZED);
    }
}
