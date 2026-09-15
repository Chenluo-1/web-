package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 我的优惠券 VO（用户券 + 模板信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponVo {
    /** 用户券id（下单时传这个） */
    private Long userCouponId;
    private Long couponId;
    private String name;
    private Integer type;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Date endTime;
    /** 用户券状态(0未使用 1已使用 2已过期) */
    private Integer status;
}
