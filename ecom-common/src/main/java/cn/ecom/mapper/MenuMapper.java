package cn.ecom.mapper;

import cn.ecom.domain.pojo.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MenuMapper extends BaseMapper<Menu> {

    /** 查询用户权限标识集合（sys_menu.permit） */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询用户可见菜单（C/M 级，扁平列表，由 service 组装成树） */
    List<Menu> selectRouterMenuTreeByUserId(@Param("userId") Long userId);
}
