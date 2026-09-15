package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车项 VO（含 SKU/商品信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVo {
    private Long id;
    private Long skuId;
    private Long productId;
    private String title;
    private String image;
    private String specs;
    private BigDecimal price;
    private Integer stock;
    private Integer quantity;
    private Integer checked;
    /** 商品是否在上架（下架商品结算时提示） */
    private Integer productStatus;
}
