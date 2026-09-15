package cn.ecom.controller;

import cn.ecom.domain.pojo.Menu;
import cn.ecom.service.MenuService;
import cn.ecom.utils.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单查询（角色分配权限树使用）
 */
@RestController
@RequestMapping("/sys/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** 全量菜单树（M/C/F 全部，供角色分配勾选） */
    @GetMapping("/tree")
    @PreAuthorize("@ps.hasPermission('sys:role')")
    public ResponseResult<List<Menu>> tree() {
        return ResponseResult.okResult(menuService.selectAllMenuTree());
    }
}
