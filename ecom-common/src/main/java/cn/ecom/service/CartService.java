package cn.ecom.service;

import cn.ecom.domain.pojo.Cart;
import cn.ecom.domain.vo.CartItemVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CartService extends IService<Cart> {

    /** 当前用户购物车（含 SKU/商品信息） */
    List<CartItemVo> listMine();

    /** 加购（同 SKU 合并数量；校验库存） */
    void addCart(Long skuId, Integer quantity);

    /** 修改数量（校验库存） */
    void updateQuantity(Long id, Integer quantity);

    /** 单件勾选 */
    void updateChecked(Long id, Integer checked);

    /** 全选/全不选 */
    void checkAll(Integer checked);

    /** 删除（仅限自己的购物车项） */
    void deleteCarts(List<Long> ids);

    /** 结算用：当前用户勾选的购物车项 */
    List<CartItemVo> selectCheckedCartItems(Long userId);

    /** 支付成功后按 SKU 清理购物车 */
    void removeBySkuIds(Long userId, List<Long> skuIds);
}
