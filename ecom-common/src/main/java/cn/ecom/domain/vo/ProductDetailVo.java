package cn.ecom.domain.vo;

import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.pojo.ProductImage;
import cn.ecom.domain.pojo.ProductSpec;
import cn.ecom.domain.pojo.Sku;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品详情 VO（编辑页回显）：商品 + 规格 + SKU + 图集
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailVo {
    private Product product;
    private List<ProductSpec> specs;
    private List<Sku> skus;
    private List<ProductImage> images;
}
