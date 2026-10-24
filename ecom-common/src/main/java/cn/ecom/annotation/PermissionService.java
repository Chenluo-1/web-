package cn.ecom.annotation;

import cn.ecom.domain.pojo.LoginUser;
import cn.ecom.utils.SecurityUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限判断服务（实验4 任务二·步骤1），Bean 名 ss：
 * 方法注解 @PreAuthorize("@ss.hasPermission('user:add')")
 * <p>
 * 路径级授权（CustomAuthorizationManager）与方法注解共用本类逻辑，避免两处规则不一致。
 * 超级管理员以权限集合中的通配符 {@link #ALL_PERMISSION} 标识，直接放行。
 */
@Service("ss")
public class PermissionService {

    /** 超级管理员通配符：权限集合中含它即视为拥有全部权限 */
    public static final String ALL_PERMISSION = "*:*:*";

    /** 判断是否拥有指定权限 */
    public boolean hasPermission(String permission) {
        List<String> permissions = getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(ALL_PERMISSION) || permissions.contains(permission);
    }

    /** 判断是否拥有任意一个权限（满足其一即通过） */
    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /** 判断是否拥有指定角色 */
    public boolean hasRole(String role) {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null || loginUser.getRoles() == null || loginUser.getRoles().isEmpty()) {
            return false;
        }
        return loginUser.getRoles().contains(role);
    }

    /** 判断是否为超级管理员（权限集合含通配符） */
    public boolean isSuperAdmin() {
        List<String> permissions = getPermissions();
        return permissions != null && permissions.contains(ALL_PERMISSION);
    }

    /** 当前登录用户的权限集合；取不到或为空返回 null */
    private List<String> getPermissions() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getPermissions();
    }

    /**
     * 从安全上下文取登录用户：未登录、匿名身份、principal 非 LoginUser 一律返回 null。
     * 注意不能裸强转——匿名态 principal 是字符串 "anonymousUser"，直接转会 ClassCastException。
     */
    private LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityUtils.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser ? (LoginUser) principal : null;
    }
}
