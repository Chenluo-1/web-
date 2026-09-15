package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 购物车表实体
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_cart")
public class Cart implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long skuId;

    private Integer quantity;

    /** 选中状态(0未选中 1选中) */
    private Integer checked;

    private Date createTime;

    private Date updateTime;

    // 不映射 del_flag：购物车为物理删除（清空后允许同 SKU 再次加购，避免 uk_user_sku 唯一键冲突；
    // 全局逻辑删除按实体属性名生效，实体不含该属性即物理删除）
}
