package cn.ecom.filter;

import cn.ecom.domain.pojo.LoginUser;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.JwtUtil;
import cn.ecom.utils.RedisCache;
import cn.ecom.utils.ResponseResult;
import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * token 校验过滤器（照 seig）：
 * 请求头 authorization 携带 token → 解析用户 id → Redis 取登录态 → 写入 SecurityContext
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    /** Redis 中登录态的 key 前缀 */
    private static final String LOGIN_USER_KEY = "user:";

    private final RedisCache redisCache;

    public JwtAuthenticationTokenFilter(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader("authorization");
        if (!StringUtils.hasText(token)) {
            // 无 token 放行，由 Security 决定是否需要登录
            filterChain.doFilter(request, response);
            return;
        }

        String userId;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userId = claims.getSubject();
        } catch (Exception e) {
            renderUnauthorized(response, "token无效或已过期");
            return;
        }

        LoginUser loginUser = redisCache.getCacheObject(LOGIN_USER_KEY + userId);
        if (loginUser == null || !token.equals(loginUser.getToken())) {
            renderUnauthorized(response, "token无效或已过期");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void renderUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.getWriter().write(JSON.toJSONString(
                ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, msg)));
    }
}
