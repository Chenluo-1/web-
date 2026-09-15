package cn.ecom.service.impl;

import cn.ecom.domain.pojo.LoginUser;
import cn.ecom.domain.pojo.Menu;
import cn.ecom.domain.pojo.User;
import cn.ecom.domain.vo.RoutersVo;
import cn.ecom.domain.vo.UserDetailInfoVo;
import cn.ecom.domain.vo.UserInfoVo;
import cn.ecom.service.LoginService;
import cn.ecom.service.MenuService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.JwtUtil;
import cn.ecom.utils.RedisCache;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.SecurityUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 登录服务实现
 */
@Service
public class LoginServiceImpl implements LoginService {

    /** Redis 中登录态的 key 前缀 */
    private static final String LOGIN_USER_KEY = "user:";

    private final AuthenticationManager authenticationManager;
    private final RedisCache redisCache;
    private final MenuService menuService;

    public LoginServiceImpl(AuthenticationManager authenticationManager, RedisCache redisCache, MenuService menuService) {
        this.authenticationManager = authenticationManager;
        this.redisCache = redisCache;
        this.menuService = menuService;
    }

    @Override
    public ResponseResult<UserInfoVo> login(User user) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseResult.errorResult(AppHttpCodeEnum.USERNAME_OR_PASSWORD_ERROR);
        } catch (DisabledException e) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            return ResponseResult.errorResult(AppHttpCodeEnum.USERNAME_OR_PASSWORD_ERROR, e.getMessage());
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Long userId = loginUser.getUser().getId();

        // 生成 token 并写入 Redis 登录态（TTL 与 JWT 有效期一致）
        String jwt = JwtUtil.createJWT(userId.toString());
        loginUser.setToken(jwt);
        loginUser.getUser().setPassword(null);
        redisCache.setCacheObject(LOGIN_USER_KEY + userId, loginUser,
                JwtUtil.JWT_TTL.intValue() / 1000, TimeUnit.SECONDS);

        return ResponseResult.okResult(new UserInfoVo(loginUser.getUser(), jwt));
    }

    @Override
    public ResponseResult<Void> logout() {
        Long userId = SecurityUtils.getUserId();
        redisCache.deleteObject(LOGIN_USER_KEY + userId);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult<UserDetailInfoVo> getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        User user = loginUser.getUser();
        user.setPassword(null);
        return ResponseResult.okResult(
                new UserDetailInfoVo(user, loginUser.getPermissions(), loginUser.getRoles()));
    }

    @Override
    public ResponseResult<RoutersVo> getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<Menu> menus = menuService.selectRouterMenuTreeByUserId(userId);
        return ResponseResult.okResult(new RoutersVo(menus));
    }
}
