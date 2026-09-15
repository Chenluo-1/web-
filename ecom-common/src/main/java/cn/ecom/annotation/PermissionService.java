package cn.ecom.annotation;

import cn.ecom.domain.pojo.LoginUser;
import cn.ecom.handler.exception.LoginRequiredException;
import cn.ecom.utils.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * 权限校验服务：@PreAuthorize("@ps.hasPermission('sys:user:add')")
 * super_admin 角色直通（双保险，菜单分配之外）
 */
@Service("ps")
public class PermissionService {

    public boolean hasPermission(String permission) {
        LoginUser loginUser;
        try {
            loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null) {
                throw new LoginRequiredException("请先登录");
            }
        } catch (LoginRequiredException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginRequiredException("请先登录");
        }
        if (loginUser.getRoles() != null && loginUser.getRoles().contains("super_admin")) {
            return true;
        }
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permission);
    }
}
