package cn.ecom.service;

import cn.ecom.domain.pojo.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /** 全量类目树 */
    List<Category> selectCategoryTree();

    /** 新增类目（自动计算层级，最多三级） */
    void addCategory(Category category);

    /** 修改类目（名称/排序/图标/状态，不支持换父级） */
    void updateCategory(Category category);

    /** 删除类目（有子类目或有商品时拒绝） */
    void deleteCategory(Long id);
}
