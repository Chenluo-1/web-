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

    public RoleServiceImpl(RoleMenuMapper roleMenuMapper, UserRoleMapper userRoleMapper) {
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
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
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
    }
}
