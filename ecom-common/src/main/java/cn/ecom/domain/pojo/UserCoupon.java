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
 * 用户优惠券表实体（status：0未使用 1已使用 2已过期）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_user_coupon")
public class UserCoupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponId;

    private Integer status;

    private Date receiveTime;

    private Date useTime;

    /** 使用的订单号 */
    private String orderNo;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
