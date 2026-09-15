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
 * 订单主表实体
 * 状态：0待支付 1待发货 2运输中 3已完成 4已取消 5退款中 6已退款
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_order")
public class Order implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 使用的用户券id */
    private Long userCouponId;

    private Integer status;

    /** 收货信息快照 */
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    /** 支付方式(0模拟支付) */
    private Integer payType;

    private Date payTime;
    private Date shipTime;
    private Date receiveTime;
    private Date cancelTime;

    private String remark;

    /** 幂等键(防重复提交) */
    private String idempotencyKey;

    /** 支付超时时间 */
    private Date expireTime;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
