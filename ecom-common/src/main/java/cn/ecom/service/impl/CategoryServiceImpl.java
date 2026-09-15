package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Category;
import cn.ecom.domain.pojo.Product;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.CategoryMapper;
import cn.ecom.mapper.ProductMapper;
import cn.ecom.service.CategoryService;
import cn.ecom.utils.AppHttpCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 类目服务实现：三级树 + 删除约束
 */
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    /** 类目最大层级 */
    private static final int MAX_LEVEL = 3;

    private final ProductMapper productMapper;

    public CategoryServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public List<Category> selectCategoryTree() {
        List<Category> list = list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getParentId)
                .orderByAsc(Category::getSort));
        return buildTree(list, 0L);
    }

    private List<Category> buildTree(List<Category> list, Long parentId) {
        return list.stream()
                .filter(c -> c.getParentId().equals(parentId))
                .map(c -> c.setChildren(buildTree(list, c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public void addCategory(Category category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "类目名称不能为空");
        }
        category.setId(null);
        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        category.setParentId(parentId);

        int level = 1;
        if (parentId != 0L) {
            Category parent = getById(parentId);
            if (parent == null) {
                throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "父类目不存在");
            }
            level = (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
        }
        if (level > MAX_LEVEL) {
            throw new BusinessException(AppHttpCodeEnum.CATEGORY_LEVEL_LIMIT);
        }
        category.setLevel(level);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(0);
        }
        save(category);
    }

    @Override
    public void updateCategory(Category category) {
        if (category.getId() == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "缺少类目id");
        }
        // 不支持换父级（避免整棵子树的层级重算），parentId/level 传空即被 MP 忽略
        category.setParentId(null);
        category.setLevel(null);
        updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Integer children = lambdaQuery().eq(Category::getParentId, id).count();
        if (children != null && children > 0) {
            throw new BusinessException(AppHttpCodeEnum.CATEGORY_HAS_CHILD);
        }
        Integer products = productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (products != null && products > 0) {
            throw new BusinessException(AppHttpCodeEnum.CATEGORY_HAS_PRODUCT);
        }
        removeById(id);
    }
}
