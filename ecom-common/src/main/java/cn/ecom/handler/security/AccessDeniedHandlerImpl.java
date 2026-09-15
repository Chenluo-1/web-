package cn.ecom.handler.security;

import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.WebUtils;
import com.alibaba.fastjson.JSON;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 授权失败处理器：已登录但无权限 → HTTP 403 + 统一响应体
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        ResponseResult<Void> result = ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATION_AUTHORITY);
        WebUtils.renderString(response, JSON.toJSONString(result), HttpServletResponse.SC_FORBIDDEN);
    }
}
