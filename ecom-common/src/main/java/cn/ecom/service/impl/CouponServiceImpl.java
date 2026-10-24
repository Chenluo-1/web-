package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Coupon;
import cn.ecom.domain.pojo.UserCoupon;
import cn.ecom.domain.vo.UserCouponVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.CouponMapper;
import cn.ecom.mapper.UserCouponMapper;
import cn.ecom.service.CouponService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现
 */
@Service("couponService")
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    private final UserCouponMapper userCouponMapper;

    public CouponServiceImpl(UserCouponMapper userCouponMapper) {
        this.userCouponMapper = userCouponMapper;
    }

    // ---------- 消费者端 ----------

    @Override
    public List<Coupon> listAvailable() {
        Date now = new Date();
        List<Coupon> coupons = list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 0)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .orderByAsc(Coupon::getThresholdAmount));
        markReceived(coupons);
        return coupons;
    }

    /**
     * 标记当前用户在这批券上是否已领满：一次 in 查询，避免前端逐个试探。
     * 领券中心据此直接展示「已领取」，点击时也能先拦一层。
     */
    private void markReceived(List<Coupon> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            return;
        }
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<UserCoupon> mine = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, SecurityUtils.getUserId())
                .in(UserCoupon::getCouponId, couponIds));
        Map<Long, Long> owned = new HashMap<>();
        for (UserCoupon userCoupon : mine) {
            owned.merge(userCoupon.getCouponId(), 1L, Long::sum);
        }
        for (Coupon coupon : coupons) {
            int limit = coupon.getPerUserLimit() == null || coupon.getPerUserLimit() < 1 ? 1 : coupon.getPerUserLimit();
            coupon.setReceived(owned.getOrDefault(coupon.getId(), 0L) >= limit);
        }
    }

    @Override
    @Transactional
    public void receive(Long couponId) {
        Coupon coupon = getById(couponId);
        if (coupon == null || coupon.getStatus() != 0) {
            throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID, "优惠券不存在或已停用");
        }
        Date now = new Date();
        if (coupon.getStartTime() != null && now.before(coupon.getStartTime())) {
            throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID, "优惠券尚未开始发放");
        }
        if (coupon.getEndTime() != null && now.after(coupon.getEndTime())) {
            throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID, "优惠券已过期");
        }
        if (coupon.getTotalCount() != null && coupon.getTotalCount() > 0
                && coupon.getReceivedCount() != null && coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID, "优惠券已被领完");
        }
        Long userId = SecurityUtils.getUserId();
        // 限领校验：该用户此券的非删除持有记录数（未使用+已使用+已过期都算持有过）
        Integer received = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        int limit = coupon.getPerUserLimit() == null || coupon.getPerUserLimit() < 1 ? 1 : coupon.getPerUserLimit();
        if (received != null && received >= limit) {
            String msg = limit <= 1 ? "该优惠券您已领取，不能重复领取"
                    : "该优惠券每人限领 " + limit + " 张，您已领满";
            throw new BusinessException(AppHttpCodeEnum.COUPON_RECEIVED_LIMIT, msg);
        }
        userCouponMapper.insert(new UserCoupon()
                .setUserId(userId)
                .setCouponId(couponId)
                .setStatus(0)
                .setReceiveTime(now));
        updateById(new Coupon().setId(couponId).setReceivedCount((coupon.getReceivedCount() == null ? 0 : coupon.getReceivedCount()) + 1));
    }

    @Override
    public List<UserCouponVo> listMine() {
        Long userId = SecurityUtils.getUserId();
        List<UserCoupon> mine = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .orderByAsc(UserCoupon::getReceiveTime));
        Date now = new Date();
        List<UserCouponVo> result = new ArrayList<>();
        for (UserCoupon userCoupon : mine) {
            Coupon coupon = getById(userCoupon.getCouponId());
            if (coupon == null || coupon.getStatus() != 0) {
                continue;
            }
            if (coupon.getEndTime() != null && now.after(coupon.getEndTime())) {
                continue;
            }
            result.add(new UserCouponVo(userCoupon.getId(), coupon.getId(), coupon.getName(), coupon.getType(),
                    coupon.getThresholdAmount(), coupon.getDiscountAmount(), coupon.getDiscountRate(),
                    coupon.getEndTime(), userCoupon.getStatus()));
        }
        return result;
    }

    // ---------- 管理端 ----------

    @Override
    public Page<Coupon> selectByPage(Integer pageNum, Integer pageSize) {
        return page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getId));
    }

    @Override
    public void addCoupon(Coupon coupon) {
        validate(coupon);
        coupon.setId(null);
        coupon.setReceivedCount(0);
        if (coupon.getStatus() == null) {
            coupon.setStatus(0);
        }
        save(coupon);
    }

    @Override
    public void updateCoupon(Coupon coupon) {
        if (coupon.getId() == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "缺少优惠券id");
        }
        updateById(coupon);
    }

    @Override
    public void deleteCoupons(List<Long> ids) {
        removeByIds(ids);
    }

    private void validate(Coupon coupon) {
        if (!StringUtils.hasText(coupon.getName())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "优惠券名称不能为空");
        }
        if (coupon.getThresholdAmount() == null || coupon.getThresholdAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "使用门槛不能为负");
        }
        if (coupon.getType() == null) {
            coupon.setType(0);
        }
        if (coupon.getType() == 0 && (coupon.getDiscountAmount() == null
                || coupon.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "满减金额必须大于 0");
        }
        if (coupon.getStartTime() == null || coupon.getEndTime() == null
                || !coupon.getEndTime().after(coupon.getStartTime())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "有效期不合法");
        }
    }
}
