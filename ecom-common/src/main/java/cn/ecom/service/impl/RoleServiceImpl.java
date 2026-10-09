package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Role;
import cn.ecom.domain.pojo.RoleMenu;
import cn.ecom.domain.pojo.UserRole;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.RoleMapper;
import cn.ecom.mapper.RoleMenuMapper;
import cn.ecom.mapper.UserRoleMapper;
import cn.ecom.service.RoleService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.RedisCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service("roleService")
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    private final RedisCache redisCache;

    public RoleServiceImpl(RoleMenuMapper roleMenuMapper, UserRoleMapper userRoleMapper, RedisCache redisCache) {
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
        this.redisCache = redisCache;
    }

    @Override
    public Page<Role> selectByPage(Integer pageNum, Integer pageSize) {
        return page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getRoleSort));
    }

    @Override
    public void addRole(Role role) {
        if (!StringUtils.hasText(role.getRoleName()) || !StringUtils.hasText(role.getRoleKey())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "角色名称和权限字符不能为空");
        }
        Integer count = lambdaQuery().eq(Role::getRoleKey, role.getRoleKey()).count();
        if (count != null && count > 0) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "角色权限字符已存在");
        }
        // 清掉占用该权限字符的已删角色（逻辑删除的行查询看不到，却仍占着 uk_role_key）
        getBaseMapper().purgeDeletedByRoleKey(role.getRoleKey());
        role.setId(null);
        save(role);
    }

    @Override
    public void updateRole(Role role) {
        if (role.getId() == null) {
            throw new BusinessException(AppHttpCodeEnum.ROLE_NOT_EXIST);
        }
        updateById(role);
    }

    @Override
    @Transactional
    public void deleteRoles(List<Long> ids) {
        for (Long roleId : ids) {
            Integer used = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
            if (used != null && used > 0) {
                Role role = getById(roleId);
                String name = role == null ? String.valueOf(roleId) : role.getRoleName();
                throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "角色「" + name + "」已分配给用户，不允许删除");
            }
        }
        removeByIds(ids);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, ids));
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                RoleMenu roleMenu = new RoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }
        // 菜单权限变了，持有该角色的用户登录态必须失效（否则权限要等 24h 才生效）
        for (Long userId : userIdsOfRole(roleId)) {
            redisCache.deleteObject(RedisCache.LOGIN_USER_KEY + userId);
        }
    }

    /** 持有该角色的用户 id 集合 */
    private List<Long> userIdsOfRole(Long roleId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))
                .stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
    }
}
