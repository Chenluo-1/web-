package cn.ecom.service.impl;

import cn.ecom.domain.dto.ProductDto;
import cn.ecom.domain.dto.SkuDto;
import cn.ecom.domain.dto.SpecDto;
import cn.ecom.domain.pojo.Category;
import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.pojo.ProductImage;
import cn.ecom.domain.pojo.ProductSpec;
import cn.ecom.domain.pojo.Sku;
import cn.ecom.domain.vo.ProductDetailVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.CategoryMapper;
import cn.ecom.mapper.ProductImageMapper;
import cn.ecom.mapper.ProductMapper;
import cn.ecom.mapper.ProductSpecMapper;
import cn.ecom.mapper.SkuMapper;
import cn.ecom.service.ProductService;
import cn.ecom.utils.AppHttpCodeEnum;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品服务实现：SKU 对账保存、价格区间、库存为 0 自动下架
 */
@Service("productService")
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final CategoryMapper categoryMapper;
    private final SkuMapper skuMapper;
    private final ProductSpecMapper specMapper;
    private final ProductImageMapper imageMapper;

    public ProductServiceImpl(CategoryMapper categoryMapper, SkuMapper skuMapper,
                              ProductSpecMapper specMapper, ProductImageMapper imageMapper) {
        this.categoryMapper = categoryMapper;
        this.skuMapper = skuMapper;
        this.specMapper = specMapper;
        this.imageMapper = imageMapper;
    }

    @Override
    public Page<Product> selectProductByPage(Integer pageNum, Integer pageSize, String title, Long categoryId, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), Product::getTitle, title);
        if (categoryId != null) {
            wrapper.in(Product::getCategoryId, collectCategoryIds(categoryId));
        }
        wrapper.eq(status != null, Product::getStatus, status);
        wrapper.orderByDesc(Product::getId);
        return page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize), wrapper);
    }

    /** 类目筛选：包含该节点及所有子孙类目 */
    private List<Long> collectCategoryIds(Long rootId) {
        List<Category> all = categoryMapper.selectList(null);
        List<Long> result = new ArrayList<>();
        result.add(rootId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Category category : all) {
                if (result.contains(category.getParentId()) && !result.contains(category.getId())) {
                    result.add(category.getId());
                    changed = true;
                }
            }
        }
        return result;
    }

    @Override
    public ProductDetailVo getProductDetail(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException(AppHttpCodeEnum.PRODUCT_NOT_EXIST);
        }
        List<ProductSpec> specs = specMapper.selectList(new LambdaQueryWrapper<ProductSpec>()
                .eq(ProductSpec::getProductId, id).orderByAsc(ProductSpec::getSort));
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, id).orderByAsc(Sku::getId));
        List<ProductImage> images = imageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, id).orderByAsc(ProductImage::getSort));
        return new ProductDetailVo(product, specs, skus, images);
    }

    @Override
    @Transactional
    public void addProduct(ProductDto productDto) {
        validate(productDto);
        productDto.setId(null);
        productDto.setSales(0);
        productDto.setStatus(0);
        productDto.setPriceMin(BigDecimal.ZERO);
        productDto.setPriceMax(BigDecimal.ZERO);
        save(productDto);
        saveSpecs(productDto.getId(), productDto.getSpecs());
        saveSkus(productDto.getId(), productDto.getSkus());
        saveImages(productDto.getId(), productDto.getImages());
        refreshPriceAndStatus(productDto.getId());
    }

    @Override
    @Transactional
    public void updateProduct(ProductDto productDto) {
        if (productDto.getId() == null) {
            throw new BusinessException(AppHttpCodeEnum.PRODUCT_NOT_EXIST);
        }
        validate(productDto);
        // 销量不允许前端修改，状态由上下架接口管理
        productDto.setSales(null);
        updateById(productDto);
        saveSpecs(productDto.getId(), productDto.getSpecs());
        saveSkus(productDto.getId(), productDto.getSkus());
        saveImages(productDto.getId(), productDto.getImages());
        refreshPriceAndStatus(productDto.getId());
    }

    private void validate(ProductDto productDto) {
        if (!StringUtils.hasText(productDto.getTitle())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "商品标题不能为空");
        }
        if (productDto.getCategoryId() == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "请选择商品类目");
        }
        Category category = categoryMapper.selectById(productDto.getCategoryId());
        if (category == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "类目不存在");
        }
        if (productDto.getSkus() == null || productDto.getSkus().isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "至少需要一个 SKU");
        }
    }

    /** 规格维度：整体替换 */
    private void saveSpecs(Long productId, List<SpecDto> specs) {
        specMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, productId));
        if (specs == null || specs.isEmpty()) {
            return;
        }
        int sort = 0;
        for (SpecDto specDto : specs) {
            if (!StringUtils.hasText(specDto.getSpecName())) {
                continue;
            }
            ProductSpec entity = new ProductSpec()
                    .setProductId(productId)
                    .setSpecName(specDto.getSpecName())
                    .setSpecValues(JSON.toJSONString(specDto.getValues() == null ? Collections.emptyList() : specDto.getValues()))
                    .setSort(sort++);
            specMapper.insert(entity);
        }
    }

    /** SKU 按 id 对账：有 id 更新、无 id 新增、本次未提交的旧 SKU 逻辑删除（保留已填价格/库存） */
    private void saveSkus(Long productId, List<SkuDto> skus) {
        List<Sku> existing = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, productId));
        Set<Long> existingIds = existing.stream().map(Sku::getId).collect(Collectors.toSet());
        Set<Long> keepIds = new HashSet<>();

        if (skus != null) {
            for (SkuDto skuDto : skus) {
                Sku sku = new Sku()
                        .setProductId(productId)
                        .setSpecs(skuDto.getSpecs() == null ? "{}" : JSON.toJSONString(skuDto.getSpecs()))
                        .setSkuCode(skuDto.getSkuCode())
                        .setPrice(skuDto.getPrice() == null ? BigDecimal.ZERO : skuDto.getPrice())
                        .setStock(skuDto.getStock() == null ? 0 : skuDto.getStock())
                        .setImage(skuDto.getImage());
                if (skuDto.getId() != null && existingIds.contains(skuDto.getId())) {
                    sku.setId(skuDto.getId());
                    skuMapper.updateById(sku);
                    keepIds.add(skuDto.getId());
                } else {
                    sku.setSales(0).setStatus(1).setVersion(0);
                    skuMapper.insert(sku);
                    keepIds.add(sku.getId());
                }
            }
        }

        List<Long> removeIds = existing.stream()
                .map(Sku::getId)
                .filter(id -> !keepIds.contains(id))
                .collect(Collectors.toList());
        if (!removeIds.isEmpty()) {
            skuMapper.deleteBatchIds(removeIds);
        }
    }

    /** 图集：整体替换 */
    private void saveImages(Long productId, List<String> images) {
        imageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        if (images == null || images.isEmpty()) {
            return;
        }
        int sort = 0;
        for (String url : images) {
            if (!StringUtils.hasText(url)) {
                continue;
            }
            ProductImage image = new ProductImage().setProductId(productId).setUrl(url).setSort(sort++);
            imageMapper.insert(image);
        }
    }

    /** 重算价格区间；所有 SKU 库存为 0 时自动下架 */
    private void refreshPriceAndStatus(Long productId) {
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, productId));
        if (skus.isEmpty()) {
            return;
        }
        Product update = new Product().setId(productId);
        BigDecimal min = skus.stream().map(Sku::getPrice).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = skus.stream().map(Sku::getPrice).filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        update.setPriceMin(min).setPriceMax(max);

        int totalStock = skus.stream().mapToInt(s -> s.getStock() == null ? 0 : s.getStock()).sum();
        if (totalStock <= 0) {
            update.setStatus(0);
        }
        updateById(update);
    }

    @Override
    @Transactional
    public void deleteProducts(List<Long> ids) {
        removeByIds(ids);
        skuMapper.delete(new LambdaQueryWrapper<Sku>().in(Sku::getProductId, ids));
        specMapper.delete(new LambdaQueryWrapper<ProductSpec>().in(ProductSpec::getProductId, ids));
        imageMapper.delete(new LambdaQueryWrapper<ProductImage>().in(ProductImage::getProductId, ids));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException(AppHttpCodeEnum.PRODUCT_NOT_EXIST);
        }
        if (status != null && status == 1) {
            int totalStock = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, id))
                    .stream().mapToInt(s -> s.getStock() == null ? 0 : s.getStock()).sum();
            if (totalStock <= 0) {
                throw new BusinessException(AppHttpCodeEnum.STOCK_NOT_ENOUGH, "所有 SKU 库存为 0，无法上架");
            }
        }
        updateById(new Product().setId(id).setStatus(status));
    }
}
