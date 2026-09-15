package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品SKU表实体（含库存，规格组合笛卡尔积）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_sku")
public class Sku implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 规格组合JSON，如 {"颜色":"红","尺码":"S"} */
    private String specs;

    private String skuCode;

    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 乐观锁版本号（P3 下单扣减时手动 SQL 使用，不走 MP 乐观锁插件） */
    private Integer version;

    /** SKU图 */
    private String image;

    private Integer sales;

    /** 状态(0停用 1启用) */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
