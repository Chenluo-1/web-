package cn.ecom.service;

import cn.ecom.domain.pojo.Role;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RoleService extends IService<Role> {

    Page<Role> selectByPage(Integer pageNum, Integer pageSize);

    void addRole(Role role);

    void updateRole(Role role);

    void deleteRoles(List<Long> ids);

    /** 角色已分配的菜单 id 集合 */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /** 给角色分配菜单（先删后插） */
    void assignMenus(Long roleId, List<Long> menuIds);
}
