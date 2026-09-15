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
 * 商品SPU表实体
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_product")
public class Product implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 类目id(三级类目) */
    private Long categoryId;

    /** 主图URL */
    private String mainImage;

    /** 商品详情(富文本) */
    private String description;

    /** 上下架(0下架 1上架) */
    private Integer status;

    /** 价格区间下限(冗余自SKU) */
    private BigDecimal priceMin;

    /** 价格区间上限(冗余自SKU) */
    private BigDecimal priceMax;

    private Integer sales;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
