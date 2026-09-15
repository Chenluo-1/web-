package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.pojo.Category;
import cn.ecom.service.CategoryService;
import cn.ecom.utils.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 类目管理（/product/category）
 */
@RestController
@RequestMapping("/product/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 全量类目树（登录即可，商品编辑选类目也要用） */
    @GetMapping
    public ResponseResult<List<Category>> tree() {
        return ResponseResult.okResult(categoryService.selectCategoryTree());
    }

    @GetMapping("/{id}")
    public ResponseResult<Category> selectOne(@PathVariable Long id) {
        return ResponseResult.okResult(categoryService.getById(id));
    }

    @OperLog(module = "类目管理", type = "新增")
    @PreAuthorize("@ps.hasPermission('product:category:add')")
    @PostMapping
    public ResponseResult<Void> insert(@RequestBody Category category) {
        categoryService.addCategory(category);
        return ResponseResult.okResult();
    }

    @OperLog(module = "类目管理", type = "修改")
    @PreAuthorize("@ps.hasPermission('product:category:edit')")
    @PutMapping
    public ResponseResult<Void> update(@RequestBody Category category) {
        categoryService.updateCategory(category);
        return ResponseResult.okResult();
    }

    @OperLog(module = "类目管理", type = "删除")
    @PreAuthorize("@ps.hasPermission('product:category:del')")
    @DeleteMapping("/{id}")
    public ResponseResult<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseResult.okResult();
    }
}
