package cn.ecom.service;

import cn.ecom.domain.dto.ProductDto;
import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.vo.ProductDetailVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductService extends IService<Product> {

    /** 分页查询（标题模糊 / 类目含子孙 / 状态筛选） */
    Page<Product> selectProductByPage(Integer pageNum, Integer pageSize, String title, Long categoryId, Integer status);

    /** 商品详情（编辑页回显：商品+规格+SKU+图集） */
    ProductDetailVo getProductDetail(Long id);

    /** 新增商品（含规格/SKU/图集），默认下架 */
    void addProduct(ProductDto productDto);

    /** 修改商品（SKU 按 id 对账：更新/新增/删除，不丢已有价格库存） */
    void updateProduct(ProductDto productDto);

    /** 删除商品（含规格/SKU/图集） */
    void deleteProducts(List<Long> ids);

    /** 上下架（上架时校验有库存；库存为 0 的商品不允许上架） */
    void updateStatus(Long id, Integer status);
}
