package cn.ecom.service;

import cn.ecom.domain.pojo.Coupon;
import cn.ecom.domain.vo.UserCouponVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CouponService extends IService<Coupon> {

    // ---------- 消费者端 ----------

    /** 可领取的券（有效期内的正常模板） */
    List<Coupon> listAvailable();

    /** 领取优惠券（校验限领/库存/有效期） */
    void receive(Long couponId);

    /** 我的券（未使用） */
    List<UserCouponVo> listMine();

    // ---------- 管理端（活动运营） ----------

    Page<Coupon> selectByPage(Integer pageNum, Integer pageSize);

    void addCoupon(Coupon coupon);

    void updateCoupon(Coupon coupon);

    void deleteCoupons(List<Long> ids);
}
