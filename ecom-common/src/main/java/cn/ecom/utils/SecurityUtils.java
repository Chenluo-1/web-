package cn.ecom.utils;

import cn.ecom.domain.pojo.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 */
public class SecurityUtils {

    /** 获取当前登录用户 */
    public static LoginUser getLoginUser() {
        return (LoginUser) getAuthentication().getPrincipal();
    }

    /** 获取 Authentication */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** 获取当前登录用户 id */
    public static Long getUserId() {
        return getLoginUser().getUser().getId();
    }
}
