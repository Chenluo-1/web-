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
 * 订单明细表实体（商品快照）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_order_item")
public class OrderItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long productId;

    private Long skuId;

    private String productName;

    private String productImage;

    private String specs;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
