package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.pojo.Role;
import cn.ecom.service.RoleService;
import cn.ecom.utils.ResponseResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理（/sys/role 标准 CRUD + 菜单分配）
 */
@RestController
@RequestMapping("/sys/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** 分页查询 */
    @GetMapping
    @PreAuthorize("@ss.hasPermission('sys:role')")
    public ResponseResult<Page<Role>> selectByPage(Integer pageNum, Integer pageSize) {
        return ResponseResult.okResult(roleService.selectByPage(pageNum, pageSize));
    }

    /** 查询全部（下拉用） */
    @GetMapping("/getAll")
    public ResponseResult<List<Role>> selectAll() {
        return ResponseResult.okResult(roleService.list());
    }

    /** 按 id 查询 */
    @GetMapping("/{id}")
    public ResponseResult<Role> selectOne(@PathVariable Long id) {
        return ResponseResult.okResult(roleService.getById(id));
    }

    /** 角色已分配的菜单 id 集合（分配权限回显） */
    @GetMapping("/{roleId}/menu")
    @PreAuthorize("@ss.hasPermission('sys:role')")
    public ResponseResult<List<Long>> getMenuIds(@PathVariable Long roleId) {
        return ResponseResult.okResult(roleService.getMenuIdsByRoleId(roleId));
    }

    /** 给角色分配菜单 */
    @OperLog(module = "角色管理", type = "分配权限")
    @PreAuthorize("@ss.hasPermission('sys:role:setMenu')")
    @PutMapping("/{roleId}/menu")
    public ResponseResult<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return ResponseResult.okResult();
    }

    /** 新增 */
    @OperLog(module = "角色管理", type = "新增")
    @PreAuthorize("@ss.hasPermission('sys:role:add')")
    @PostMapping
    public ResponseResult<Void> insert(@RequestBody Role role) {
        roleService.addRole(role);
        return ResponseResult.okResult();
    }

    /** 修改 */
    @OperLog(module = "角色管理", type = "修改")
    @PreAuthorize("@ss.hasPermission('sys:role:edit')")
    @PutMapping
    public ResponseResult<Void> update(@RequestBody Role role) {
        roleService.updateRole(role);
        return ResponseResult.okResult();
    }

    /** 删除（支持批量） */
    @OperLog(module = "角色管理", type = "删除")
    @PreAuthorize("@ss.hasPermission('sys:role:del')")
    @DeleteMapping("/{ids}")
    public ResponseResult<Void> delete(@PathVariable("ids") List<Long> ids) {
        roleService.deleteRoles(ids);
        return ResponseResult.okResult();
    }
}
