package cn.ecom.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * SKU DTO：随商品保存一起提交。
 * id 为空表示新增；specs 为规格组合键值对（如 {"颜色":"红","尺码":"S"}）
 */
@Data
public class SkuDto {
    private Long id;
    private Map<String, String> specs;
    private String skuCode;
    private BigDecimal price;
    private Integer stock;
    private String image;
}
