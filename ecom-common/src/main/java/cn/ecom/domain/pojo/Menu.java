package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单表实体（M目录 C菜单 F按钮）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_menu")
public class Menu implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单id(0为根) */
    private Long parentId;

    /** 菜单名 */
    private String name;

    /** 菜单类型(M目录 C菜单 F按钮) */
    private String menuType;

    /** 路由路径 */
    private String urlPath;

    /** 前端组件名 */
    private String component;

    /** 权限标识(模块:操作) */
    private String permit;

    private String icon;

    private Integer orderNum;

    /** 显示状态(0显示 1隐藏) */
    private Integer visible;

    /** 菜单状态(0正常 1停用) */
    private Integer status;

    @TableLogic
    private Integer delFlag;

    /** 子菜单（非表字段） */
    @TableField(exist = false)
    private List<Menu> children;
}
