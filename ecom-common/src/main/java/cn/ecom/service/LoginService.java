package cn.ecom.service;

import cn.ecom.domain.pojo.User;
import cn.ecom.domain.vo.RoutersVo;
import cn.ecom.domain.vo.UserDetailInfoVo;
import cn.ecom.domain.vo.UserInfoVo;
import cn.ecom.utils.ResponseResult;

public interface LoginService {

    /** 账号密码登录，返回 token */
    ResponseResult<UserInfoVo> login(User user);

    /** 退出登录（删除 Redis 登录态） */
    ResponseResult<Void> logout();

    /** 当前用户信息 + 权限标识集合 + 角色集合 */
    ResponseResult<UserDetailInfoVo> getInfo();

    /** 当前用户可见菜单树 */
    ResponseResult<RoutersVo> getRouters();
}
