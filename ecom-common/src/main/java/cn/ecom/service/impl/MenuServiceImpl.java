package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Menu;
import cn.ecom.mapper.MenuMapper;
import cn.ecom.service.MenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现（照 seig 的树构建逻辑）
 */
@Service("menuService")
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<String> selectPermsByUserId(Long userId) {
        return getBaseMapper().selectPermsByUserId(userId);
    }

    @Override
    public List<Menu> selectRouterMenuTreeByUserId(Long userId) {
        List<Menu> menus = getBaseMapper().selectRouterMenuTreeByUserId(userId);
        return builderMenuTree(menus, 0L);
    }

    @Override
    public List<Menu> selectAllMenuTree() {
        List<Menu> menus = list();
        return builderMenuTree(menus, 0L);
    }

    /** 找出 parentId 为指定值的一级菜单，递归挂子菜单 */
    private List<Menu> builderMenuTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> menu.getParentId().equals(parentId))
                .map(menu -> menu.setChildren(getChildren(menu, menus)))
                .collect(Collectors.toList());
    }

    private List<Menu> getChildren(Menu menu, List<Menu> menus) {
        return menus.stream()
                .filter(m -> m.getParentId().equals(menu.getId()))
                .map(m -> m.setChildren(getChildren(m, menus)))
                .collect(Collectors.toList());
    }
}
