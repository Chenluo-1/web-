package cn.ecom.domain.dto;

import cn.ecom.domain.pojo.Product;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品新增/修改 DTO：商品字段 + 规格维度 + SKU 组合 + 图集
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDto extends Product {

    /** 规格维度列表 */
    private List<SpecDto> specs;

    /** SKU 组合（笛卡尔积） */
    private List<SkuDto> skus;

    /** 图集 URL 列表 */
    private List<String> images;
}
