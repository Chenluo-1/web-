package cn.ecom.controller;

import cn.ecom.domain.pojo.User;
import cn.ecom.domain.vo.RoutersVo;
import cn.ecom.domain.vo.UserDetailInfoVo;
import cn.ecom.domain.vo.UserInfoVo;
import cn.ecom.service.LoginService;
import cn.ecom.utils.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录相关接口
 */
@RestController
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /** 账号密码登录，返回 token */
    @PostMapping("/login")
    public ResponseResult<UserInfoVo> login(@RequestBody User user) {
        return loginService.login(user);
    }

    /** 退出登录 */
    @GetMapping("/logout")
    public ResponseResult<Void> logout() {
        return loginService.logout();
    }

    /** 当前用户信息 + 权限标识集合 + 角色集合 */
    @GetMapping("/getInfo")
    public ResponseResult<UserDetailInfoVo> getInfo() {
        return loginService.getInfo();
    }

    /** 当前用户可见菜单树 */
    @GetMapping("/getRouters")
    public ResponseResult<RoutersVo> getRouters() {
        return loginService.getRouters();
    }
}
