package cn.ecom.config;

import cn.ecom.annotation.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 自定义权限管理器（实验4 任务二·步骤2）：基于请求的授权判断。
 * <p>
 * 判断分四层：
 * ① 从请求上下文取出请求对象（方法+路径）与当前认证信息；
 * ② 认证信息为空、未通过认证或主体是匿名用户 → 拒绝（未登录最终由 AuthenticationEntryPointImpl 产 401）；
 * ③ 当前用户是超级管理员（权限集合含通配符）→ 直接通过，不再逐条比对；
 * ④ 按「方法+路径」解析所需权限标识：未配置的路径放行；否则调用权限判断服务比对，命中通过，
 *    未命中拒绝并记录告警日志（已登录无权限最终由 AccessDeniedHandlerImpl 产 403）。
 * <p>
 * 纪律：
 * 1. 路径与权限的对应关系集中在本类 {@link #PATH_PERMISSION_MAP} 维护，便于审阅「哪些接口受保护」；
 * 2. 表中没配置的路径 = 放行（fail-open），即新接口默认对「已登录用户」开放——未登录一律先被第②层拦下；
 * 3. 本类只做路径级粗粒度授权，方法级细粒度仍在 Controller 的 @PreAuthorize 注解上，
 *    两者共用 PermissionService 同一套判断逻辑，规则演进时不会两处不一致；
 * 4. 拒绝时不自己写响应：401/403 由统一异常处理器输出，避免响应被重复写入。
 */
@Component
public class CustomAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthorizationManager.class);

    /** 路径 → 权限标识 对照表（"方法 路径模式" → 权限标识；Ant 通配，具体条目在前、先命中先返回） */
    private static final Map<String, String> PATH_PERMISSION_MAP = new LinkedHashMap<>();

    static {
        // —— 用户管理（实验4 任务二·步骤4 的 7 个接口 + 配套读接口）——
        PATH_PERMISSION_MAP.put("GET /sys/user", "user:list");
        PATH_PERMISSION_MAP.put("GET /sys/user/getAll", "user:list"); // 具体条目在前，防被下一条通配误判为详情
        PATH_PERMISSION_MAP.put("GET /sys/user/*", "user:query");
        PATH_PERMISSION_MAP.put("GET /sys/user/*/roleIds", "user:query");
        PATH_PERMISSION_MAP.put("POST /sys/user", "user:add");
        PATH_PERMISSION_MAP.put("PUT /sys/user", "user:edit");
        PATH_PERMISSION_MAP.put("PUT /sys/user/*/status", "user:status");
        PATH_PERMISSION_MAP.put("PUT /sys/user/respwd", "user:resetPwd");
        PATH_PERMISSION_MAP.put("DELETE /sys/user/**", "user:delete");
        // —— 角色管理 ——
        PATH_PERMISSION_MAP.put("GET /sys/role", "sys:role");
        PATH_PERMISSION_MAP.put("GET /sys/role/*/menu", "sys:role");
        PATH_PERMISSION_MAP.put("PUT /sys/role/*/menu", "sys:role:setMenu");
        PATH_PERMISSION_MAP.put("POST /sys/role", "sys:role:add");
        PATH_PERMISSION_MAP.put("PUT /sys/role", "sys:role:edit");
        PATH_PERMISSION_MAP.put("DELETE /sys/role/**", "sys:role:del");
        // —— 菜单 ——
        PATH_PERMISSION_MAP.put("GET /sys/menu/tree", "sys:role");
        // 刻意不入表（已登录即可访问，与现有注解面逐条对齐）：
        //   GET /sys/role/getAll、GET /sys/role/*（下拉/回显）、PUT /sys/user/password（本人改密）
        // 注意：切勿用 /sys/user/** 之类的宽泛模式兜底，否则会误伤 /sys/user/password 等豁免接口。
    }

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final PermissionService permissionService;

    public CustomAuthorizationManager(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        // ① 请求对象：方法 + 路径；认证信息：authentication.get()
        HttpServletRequest request = context.getRequest();
        String method = request.getMethod();
        String path = request.getServletPath();

        // ② 未登录 / 未通过认证 / 匿名身份 → 拒绝
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }

        // ③ 超级管理员直接通过
        if (permissionService.isSuperAdmin()) {
            return new AuthorizationDecision(true);
        }

        // ④ 解析所需权限标识；未配置权限要求的路径放行，否则交给权限判断服务比对
        String requiredPermission = resolvePermission(method, path);
        if (requiredPermission == null) {
            return new AuthorizationDecision(true);
        }
        if (permissionService.hasPermission(requiredPermission)) {
            return new AuthorizationDecision(true);
        }
        log.warn("权限校验未通过：用户={}，请求={} {}，所需权限={}", auth.getName(), method, path, requiredPermission);
        return new AuthorizationDecision(false);
    }

    /** 在集中表中解析「方法+路径」对应的权限标识，未配置返回 null */
    private String resolvePermission(String method, String path) {
        for (Map.Entry<String, String> entry : PATH_PERMISSION_MAP.entrySet()) {
            String key = entry.getKey();
            int split = key.indexOf(' ');
            String entryMethod = key.substring(0, split);
            String entryPattern = key.substring(split + 1);
            if (entryMethod.equalsIgnoreCase(method) && PATH_MATCHER.match(entryPattern, path)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
