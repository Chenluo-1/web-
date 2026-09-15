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
 * 优惠券模板表实体
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_coupon")
public class Coupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 类型(0满减 1折扣) */
    private Integer type;

    /** 使用门槛金额 */
    private BigDecimal thresholdAmount;

    /** 满减金额 */
    private BigDecimal discountAmount;

    /** 折扣率(如 0.85) */
    private BigDecimal discountRate;

    /** 发放总量(0不限) */
    private Integer totalCount;

    private Integer receivedCount;

    /** 每人限领 */
    private Integer perUserLimit;

    private Date startTime;

    private Date endTime;

    /** 状态(0正常 1停用) */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
