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
import java.util.Date;
import java.util.List;

/**
 * 商品类目表实体（三级树）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_category")
public class Category implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父类目id(0为根) */
    private Long parentId;

    private String name;

    /** 层级(1/2/3，最多三级) */
    private Integer level;

    private Integer sort;

    private String icon;

    /** 状态(0正常 1停用) */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;

    /** 子类目（非表字段） */
    @TableField(exist = false)
    private List<Category> children;
}
