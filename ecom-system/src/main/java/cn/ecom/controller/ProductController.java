package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.dto.ProductDto;
import cn.ecom.domain.pojo.LoginUser;
import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.vo.ProductDetailVo;
import cn.ecom.service.ProductService;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品管理（/product 标准 CRUD + 上下架）
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 分页查询（游客未指定状态时只返回已上架商品） */
    @GetMapping
    public ResponseResult<Page<Product>> selectByPage(Integer pageNum, Integer pageSize,
                                                      String title, Long categoryId, Integer status) {
        if (status == null && !(SecurityUtils.getAuthentication().getPrincipal() instanceof LoginUser)) {
            status = 1;
        }
        return ResponseResult.okResult(productService.selectProductByPage(pageNum, pageSize, title, categoryId, status));
    }

    /** 商品详情（编辑回显：商品+规格+SKU+图集） */
    @GetMapping("/{id}")
    public ResponseResult<ProductDetailVo> selectOne(@PathVariable Long id) {
        return ResponseResult.okResult(productService.getProductDetail(id));
    }

    /** 新增（默认下架，SKU 随商品一起提交） */
    @OperLog(module = "商品管理", type = "新增")
    @PreAuthorize("@ps.hasPermission('product:item:add')")
    @PostMapping
    public ResponseResult<Void> insert(@RequestBody ProductDto productDto) {
        productService.addProduct(productDto);
        return ResponseResult.okResult();
    }

    /** 修改（SKU 按 id 对账，不丢已有价格库存） */
    @OperLog(module = "商品管理", type = "修改")
    @PreAuthorize("@ps.hasPermission('product:item:edit')")
    @PutMapping
    public ResponseResult<Void> update(@RequestBody ProductDto productDto) {
        productService.updateProduct(productDto);
        return ResponseResult.okResult();
    }

    /** 上下架 */
    @OperLog(module = "商品管理", type = "上下架")
    @PreAuthorize("@ps.hasPermission('product:item:status')")
    @PutMapping("/{id}/status")
    public ResponseResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return ResponseResult.okResult();
    }

    /** 删除（支持批量） */
    @OperLog(module = "商品管理", type = "删除")
    @PreAuthorize("@ps.hasPermission('product:item:del')")
    @DeleteMapping("/{ids}")
    public ResponseResult<Void> delete(@PathVariable("ids") List<Long> ids) {
        productService.deleteProducts(ids);
        return ResponseResult.okResult();
    }
}
