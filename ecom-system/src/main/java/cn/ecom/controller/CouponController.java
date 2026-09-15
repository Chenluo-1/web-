package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.pojo.Coupon;
import cn.ecom.domain.vo.UserCouponVo;
import cn.ecom.service.CouponService;
import cn.ecom.utils.ResponseResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 优惠券：消费者端（领券/我的券）+ 管理端（活动运营）
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ---------- 消费者端 ----------

    /** 可领取的券 */
    @GetMapping("/available")
    public ResponseResult<List<Coupon>> listAvailable() {
        return ResponseResult.okResult(couponService.listAvailable());
    }

    /** 领取优惠券 */
    @PostMapping("/{couponId}/receive")
    public ResponseResult<Void> receive(@PathVariable Long couponId) {
        couponService.receive(couponId);
        return ResponseResult.okResult();
    }

    /** 我的券（未使用） */
    @GetMapping("/mine")
    public ResponseResult<List<UserCouponVo>> listMine() {
        return ResponseResult.okResult(couponService.listMine());
    }

    // ---------- 管理端 ----------

    /** 分页查询券模板 */
    @GetMapping
    @PreAuthorize("@ps.hasPermission('coupon:manage')")
    public ResponseResult<Page<Coupon>> selectByPage(Integer pageNum, Integer pageSize) {
        return ResponseResult.okResult(couponService.selectByPage(pageNum, pageSize));
    }

    @OperLog(module = "优惠券管理", type = "新增")
    @PreAuthorize("@ps.hasPermission('coupon:add')")
    @PostMapping
    public ResponseResult<Void> insert(@RequestBody Coupon coupon) {
        couponService.addCoupon(coupon);
        return ResponseResult.okResult();
    }

    @OperLog(module = "优惠券管理", type = "修改")
    @PreAuthorize("@ps.hasPermission('coupon:edit')")
    @PutMapping
    public ResponseResult<Void> update(@RequestBody Coupon coupon) {
        couponService.updateCoupon(coupon);
        return ResponseResult.okResult();
    }

    @OperLog(module = "优惠券管理", type = "删除")
    @PreAuthorize("@ps.hasPermission('coupon:del')")
    @DeleteMapping("/{ids}")
    public ResponseResult<Void> delete(@PathVariable("ids") List<Long> ids) {
        couponService.deleteCoupons(ids);
        return ResponseResult.okResult();
    }
}
