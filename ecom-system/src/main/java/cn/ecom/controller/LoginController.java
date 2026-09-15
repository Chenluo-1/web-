package cn.ecom.controller;

import cn.ecom.domain.pojo.User;
import cn.ecom.domain.vo.RoutersVo;
import cn.ecom.domain.vo.UserDetailInfoVo;
import cn.ecom.domain.vo.UserInfoVo;
import cn.ecom.service.LoginService;
import cn.ecom.service.UserService;
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
    private final UserService userService;

    public LoginController(LoginService loginService, UserService userService) {
        this.loginService = loginService;
        this.userService = userService;
    }

    /** 账号密码登录，返回 token */
    @PostMapping("/login")
    public ResponseResult<UserInfoVo> login(@RequestBody User user) {
        return loginService.login(user);
    }

    /** 消费者自助注册（自动绑定 member 角色） */
    @PostMapping("/register")
    public ResponseResult<Void> register(@RequestBody User user) {
        userService.register(user);
        return ResponseResult.okResult();
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
