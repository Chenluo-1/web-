package cn.ecom.domain.dto;

import lombok.Data;

/**
 * 下单 DTO
 */
@Data
public class OrderCreateDto {

    /** 收货地址id */
    private Long addressId;

    /** 使用的用户券id（可选） */
    private Long userCouponId;

    /** 幂等键（前端每次进入确认页生成一次） */
    private String idempotencyKey;

    private String remark;
}
