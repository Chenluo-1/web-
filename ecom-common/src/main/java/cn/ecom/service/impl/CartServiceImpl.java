package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Cart;
import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.pojo.Sku;
import cn.ecom.domain.vo.CartItemVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.CartMapper;
import cn.ecom.mapper.ProductMapper;
import cn.ecom.mapper.SkuMapper;
import cn.ecom.service.BehaviorService;
import cn.ecom.service.CartService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现
 */
@Service("cartService")
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final BehaviorService behaviorService;

    public CartServiceImpl(SkuMapper skuMapper, ProductMapper productMapper, BehaviorService behaviorService) {
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.behaviorService = behaviorService;
    }

    @Override
    public List<CartItemVo> listMine() {
        return toCartItemVos(list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, SecurityUtils.getUserId())
                .orderByDesc(Cart::getId)));
    }

    @Override
    public List<CartItemVo> selectCheckedCartItems(Long userId) {
        return toCartItemVos(list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getChecked, 1)
                .orderByDesc(Cart::getId)));
    }

    /** 组装购物车 VO（拼 SKU/商品信息） */
    private List<CartItemVo> toCartItemVos(List<Cart> carts) {
        List<CartItemVo> result = new ArrayList<>(carts.size());
        for (Cart cart : carts) {
            Sku sku = skuMapper.selectById(cart.getSkuId());
            if (sku == null) {
                continue;
            }
            Product product = productMapper.selectById(sku.getProductId());
            CartItemVo vo = new CartItemVo();
            vo.setId(cart.getId());
            vo.setSkuId(sku.getId());
            vo.setProductId(sku.getProductId());
            vo.setTitle(product == null ? "商品已删除" : product.getTitle());
            vo.setImage(StringUtils.hasText(sku.getImage())
                    ? sku.getImage()
                    : (product == null ? null : product.getMainImage()));
            vo.setSpecs(sku.getSpecs());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
            vo.setQuantity(cart.getQuantity());
            vo.setChecked(cart.getChecked());
            vo.setProductStatus(product == null ? 0 : product.getStatus());
            result.add(vo);
        }
        return result;
    }

    @Override
    public void addCart(Long skuId, Integer quantity) {
        if (skuId == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "缺少 SKU");
        }
        int qty = quantity == null || quantity < 1 ? 1 : quantity;
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(AppHttpCodeEnum.SKU_NOT_EXIST);
        }
        Long userId = SecurityUtils.getUserId();
        Cart existing = lambdaQuery().eq(Cart::getUserId, userId).eq(Cart::getSkuId, skuId).one();
        int targetQty = qty + (existing == null ? 0 : existing.getQuantity());
        if (sku.getStock() == null || sku.getStock() < targetQty) {
            throw new BusinessException(AppHttpCodeEnum.STOCK_NOT_ENOUGH,
                    "库存不足，当前仅剩 " + (sku.getStock() == null ? 0 : sku.getStock()) + " 件");
        }
        if (existing == null) {
            save(new Cart().setUserId(userId).setSkuId(skuId).setQuantity(qty).setChecked(1));
        } else {
            existing.setQuantity(targetQty);
            updateById(existing);
        }
        behaviorService.record(userId, "cart", "sku", skuId);
    }

    @Override
    public void updateQuantity(Long id, Integer quantity) {
        Cart cart = getOwnedCart(id);
        if (quantity == null || quantity < 1) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "数量至少为 1");
        }
        Sku sku = skuMapper.selectById(cart.getSkuId());
        if (sku != null && sku.getStock() != null && sku.getStock() < quantity) {
            throw new BusinessException(AppHttpCodeEnum.STOCK_NOT_ENOUGH,
                    "库存不足，当前仅剩 " + sku.getStock() + " 件");
        }
        cart.setQuantity(quantity);
        updateById(cart);
    }

    @Override
    public void updateChecked(Long id, Integer checked) {
        Cart cart = getOwnedCart(id);
        cart.setChecked(checked == null ? 1 : checked);
        updateById(cart);
    }

    @Override
    public void checkAll(Integer checked) {
        List<Cart> carts = list(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, SecurityUtils.getUserId()));
        for (Cart cart : carts) {
            cart.setChecked(checked == null ? 1 : checked);
            updateById(cart);
        }
    }

    @Override
    public void deleteCarts(List<Long> ids) {
        remove(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, SecurityUtils.getUserId())
                .in(Cart::getId, ids));
    }

    @Override
    public void removeBySkuIds(Long userId, List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        remove(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getSkuId, skuIds));
    }

    private Cart getOwnedCart(Long id) {
        Cart cart = getById(id);
        if (cart == null || !cart.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "购物车项不存在");
        }
        return cart;
    }
}
