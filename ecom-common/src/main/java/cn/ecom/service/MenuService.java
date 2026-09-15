package cn.ecom.service;

import cn.ecom.domain.pojo.Menu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MenuService extends IService<Menu> {

    /** 查询用户权限标识集合 */
    List<String> selectPermsByUserId(Long userId);

    /** 查询用户可见菜单树（M/C 级） */
    List<Menu> selectRouterMenuTreeByUserId(Long userId);

    /** 全量菜单树（M/C/F 全部，角色分配权限用） */
    List<Menu> selectAllMenuTree();
}
