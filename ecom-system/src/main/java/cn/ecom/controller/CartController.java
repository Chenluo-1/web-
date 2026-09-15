package cn.ecom.controller;

import cn.ecom.domain.vo.CartItemVo;
import cn.ecom.service.CartService;
import cn.ecom.utils.ResponseResult;
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
import java.util.Map;

/**
 * 购物车（消费者，全部按当前登录用户隔离）
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** 我的购物车 */
    @GetMapping
    public ResponseResult<List<CartItemVo>> listMine() {
        return ResponseResult.okResult(cartService.listMine());
    }

    /** 加购 {skuId, quantity} */
    @PostMapping
    public ResponseResult<Void> add(@RequestBody Map<String, Object> body) {
        Long skuId = body.get("skuId") == null ? null : Long.valueOf(body.get("skuId").toString());
        Integer quantity = body.get("quantity") == null ? 1 : Integer.valueOf(body.get("quantity").toString());
        cartService.addCart(skuId, quantity);
        return ResponseResult.okResult();
    }

    /** 修改数量 */
    @PutMapping("/{id}/quantity")
    public ResponseResult<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(id, quantity);
        return ResponseResult.okResult();
    }

    /** 单件勾选 */
    @PutMapping("/{id}/checked")
    public ResponseResult<Void> updateChecked(@PathVariable Long id, @RequestParam Integer checked) {
        cartService.updateChecked(id, checked);
        return ResponseResult.okResult();
    }

    /** 全选/全不选 */
    @PutMapping("/checkAll")
    public ResponseResult<Void> checkAll(@RequestParam Integer checked) {
        cartService.checkAll(checked);
        return ResponseResult.okResult();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public ResponseResult<Void> delete(@PathVariable("ids") List<Long> ids) {
        cartService.deleteCarts(ids);
        return ResponseResult.okResult();
    }
}
