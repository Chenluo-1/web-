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
import java.util.Date;

/**
 * 库存预警表实体（alertType：0缺货 1积压）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_stock_alert")
public class StockAlert implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

    /** 冗余商品id（便于展示） */
    private Long productId;

    /** 预警类型(0缺货 1积压) */
    private Integer alertType;

    /** 触发时库存值 */
    private Integer currentStock;

    /** 触发阈值 */
    private Integer thresholdValue;

    /** 处理状态(0未处理 1已处理) */
    private Integer status;

    private Date handleTime;

    private Long handleUserId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
