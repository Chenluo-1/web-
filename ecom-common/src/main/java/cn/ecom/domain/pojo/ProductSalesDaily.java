package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品日销量统计实体（uk: stat_date + sku_id）
 * 不映射 del_flag：T+1 重算按"物理删除当日 + 重插"覆盖
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_product_sales_daily")
public class ProductSalesDaily implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Date statDate;

    private Long productId;

    private Long skuId;

    private Integer salesQty;

    private BigDecimal salesAmount;

    private Date createTime;

    private Date updateTime;
}
