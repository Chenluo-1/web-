package cn.ecom.service.impl;

import cn.ecom.annotation.PermissionService;
import cn.ecom.domain.pojo.LoginUser;
import cn.ecom.domain.pojo.User;
import cn.ecom.mapper.MenuMapper;
import cn.ecom.mapper.RoleMapper;
import cn.ecom.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 认证用户加载（照 seig）：按登录名查用户 + 权限 + 角色
 * 超管（super_admin）权限集合直接放入通配符，由权限判断服务统一识别
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;

    public UserDetailsServiceImpl(UserMapper userMapper, MenuMapper menuMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException("用户名不存在");
        }
        List<String> roles = roleMapper.selectRoleKeyByUserId(user.getId());
        // 超管：权限集合直接放入通配符；其余角色按用户 ID 查权限标识集合
        List<String> permissions = roles != null && roles.contains("super_admin")
                ? Collections.singletonList(PermissionService.ALL_PERMISSION)
                : menuMapper.selectPermsByUserId(user.getId());
        return new LoginUser(user, permissions, roles);
    }
}
