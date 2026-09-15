package cn.ecom.utils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Web 响应工具类
 */
public class WebUtils {

    /** 将字符串渲染到客户端（HTTP 200） */
    public static void renderString(HttpServletResponse response, String string) {
        renderString(response, string, HttpServletResponse.SC_OK);
    }

    /** 将字符串渲染到客户端（指定 HTTP 状态码，用于 401/403） */
    public static void renderString(HttpServletResponse response, String string, int status) {
        try {
            response.setStatus(status);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
