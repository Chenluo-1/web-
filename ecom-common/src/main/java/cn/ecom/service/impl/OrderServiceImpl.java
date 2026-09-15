package cn.ecom.service.impl;

import cn.ecom.domain.dto.OrderCreateDto;
import cn.ecom.domain.dto.RefundAuditDto;
import cn.ecom.domain.dto.ShipDto;
import cn.ecom.domain.pojo.Address;
import cn.ecom.domain.pojo.Coupon;
import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.pojo.OrderItem;
import cn.ecom.domain.pojo.Logistics;
import cn.ecom.domain.pojo.OrderStatusLog;
import cn.ecom.domain.pojo.Refund;
import cn.ecom.domain.pojo.UserCoupon;
import cn.ecom.domain.vo.CartItemVo;
import cn.ecom.domain.vo.OrderDetailVo;
import cn.ecom.domain.vo.OrderVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.AddressMapper;
import cn.ecom.mapper.CouponMapper;
import cn.ecom.mapper.OrderItemMapper;
import cn.ecom.mapper.OrderMapper;
import cn.ecom.mapper.OrderStatusLogMapper;
import cn.ecom.mapper.RefundMapper;
import cn.ecom.mapper.SkuMapper;
import cn.ecom.mapper.UserCouponMapper;
import cn.ecom.service.CartService;
import cn.ecom.service.OrderService;
import cn.ecom.service.LogisticsService;
import cn.ecom.service.SysConfigService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现（P3 核心）：
 * - 下单即原子扣库存（防超卖），异常整体回滚
 * - 幂等键 + 唯一索引兜底防重复下单
 * - 支付/回调只做条件状态流转（幂等，不重复动库存）
 * - 超时任务取消订单并释放库存、退回优惠券
 */
@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    /** 订单状态常量 */
    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_SHIPPED = 2;
    private static final int STATUS_FINISHED = 3;
    private static final int STATUS_CANCELED = 4;
    private static final int STATUS_REFUNDING = 5;
    private static final int STATUS_REFUNDED = 6;

    private final AddressMapper addressMapper;
    private final SkuMapper skuMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final CartService cartService;
    private final SysConfigService sysConfigService;
    private final LogisticsService logisticsService;
    private final RefundMapper refundMapper;

    public OrderServiceImpl(AddressMapper addressMapper, SkuMapper skuMapper, CouponMapper couponMapper,
                            UserCouponMapper userCouponMapper, OrderItemMapper orderItemMapper,
                            OrderStatusLogMapper statusLogMapper, CartService cartService,
                            SysConfigService sysConfigService, LogisticsService logisticsService,
                            RefundMapper refundMapper) {
        this.addressMapper = addressMapper;
        this.skuMapper = skuMapper;
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.cartService = cartService;
        this.sysConfigService = sysConfigService;
        this.logisticsService = logisticsService;
        this.refundMapper = refundMapper;
    }

    @Override
    @Transactional
    public OrderVo createOrder(OrderCreateDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getIdempotencyKey())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "缺少幂等键");
        }
        Long userId = SecurityUtils.getUserId();

        // 1. 幂等预检：同一幂等键直接返回已有订单
        Order exist = getByUserIdempotencyKey(userId, dto.getIdempotencyKey());
        if (exist != null) {
            return toOrderVo(exist);
        }

        // 2. 校验地址（快照）
        Address address = dto.getAddressId() == null ? null : addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "请选择有效的收货地址");
        }

        // 3. 校验购物车勾选项
        List<CartItemVo> items = cartService.selectCheckedCartItems(userId);
        if (items.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.CART_EMPTY);
        }
        for (CartItemVo item : items) {
            if (item.getProductStatus() == null || item.getProductStatus() != 1) {
                throw new BusinessException(AppHttpCodeEnum.PRODUCT_OFF_SHELF, "「" + item.getTitle() + "」已下架，请移除后重试");
            }
        }

        // 4. 计算商品总额
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemVo item : items) {
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 5. 原子扣减库存（不足抛错→事务回滚已扣部分，防超卖）
        for (CartItemVo item : items) {
            int rows = skuMapper.deductStock(item.getSkuId(), item.getQuantity());
            if (rows == 0) {
                throw new BusinessException(AppHttpCodeEnum.STOCK_NOT_ENOUGH,
                        "「" + item.getTitle() + "」库存不足，请调整数量");
            }
        }

        // 6. 生成订单号（券核销需要先有订单号）
        String orderNo = genOrderNo();
        Date now = new Date();

        // 7. 校验并核销优惠券（条件更新防并发重复使用）
        BigDecimal discount = BigDecimal.ZERO;
        Long userCouponId = null;
        if (dto.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(dto.getUserCouponId());
            if (userCoupon == null || !userCoupon.getUserId().equals(userId) || userCoupon.getStatus() != 0) {
                throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID);
            }
            Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
            if (coupon == null || coupon.getStatus() != 0
                    || (coupon.getEndTime() != null && now.after(coupon.getEndTime()))) {
                throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID, "优惠券已失效");
            }
            if (coupon.getThresholdAmount() != null && total.compareTo(coupon.getThresholdAmount()) < 0) {
                throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID,
                        "未满足使用门槛（满 " + coupon.getThresholdAmount() + " 元可用）");
            }
            discount = calcDiscount(coupon, total);
            int rows = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, userCoupon.getId())
                    .eq(UserCoupon::getStatus, 0)
                    .set(UserCoupon::getStatus, 1)
                    .set(UserCoupon::getUseTime, now)
                    .set(UserCoupon::getOrderNo, orderNo));
            if (rows == 0) {
                throw new BusinessException(AppHttpCodeEnum.COUPON_INVALID, "优惠券已被使用");
            }
            userCouponId = userCoupon.getId();
        }

        // 8. 生成订单（待支付 + 超时时间）
        int timeoutMinutes = sysConfigService.getInt("order_timeout_minutes", 15);
        Order order = new Order()
                .setOrderNo(orderNo)
                .setUserId(userId)
                .setTotalAmount(total)
                .setDiscountAmount(discount)
                .setPayAmount(total.subtract(discount).max(BigDecimal.ZERO))
                .setUserCouponId(userCouponId)
                .setStatus(STATUS_UNPAID)
                .setReceiverName(address.getReceiverName())
                .setReceiverPhone(address.getPhone())
                .setReceiverAddress(buildFullAddress(address))
                .setRemark(dto.getRemark())
                .setIdempotencyKey(dto.getIdempotencyKey())
                .setExpireTime(new Date(now.getTime() + timeoutMinutes * 60_000L));
        save(order);

        // 9. 明细快照
        for (CartItemVo item : items) {
            orderItemMapper.insert(new OrderItem()
                    .setOrderId(order.getId())
                    .setOrderNo(orderNo)
                    .setProductId(item.getProductId())
                    .setSkuId(item.getSkuId())
                    .setProductName(item.getTitle())
                    .setProductImage(item.getImage())
                    .setSpecs(item.getSpecs())
                    .setPrice(item.getPrice())
                    .setQuantity(item.getQuantity())
                    .setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
        }

        // 10. 状态日志
        writeStatusLog(orderNo, -1, STATUS_UNPAID, 1, userId, "创建订单");
        return toOrderVo(order);
    }

    @Override
    public Order getByUserIdempotencyKey(Long userId, String idempotencyKey) {
        return lambdaQuery().eq(Order::getUserId, userId)
                .eq(Order::getIdempotencyKey, idempotencyKey)
                .one();
    }

    @Override
    public void mockPay(String orderNo) {
        doPay(orderNo);
    }

    @Override
    public String payCallback(String orderNo) {
        doPay(orderNo);
        return "success";
    }

    /** 支付核心：条件状态流转（幂等），首次流转成功才清购物车 */
    private void doPay(String orderNo) {
        Order order = getOwnedOrder(orderNo);
        if (order.getStatus() >= STATUS_PAID && order.getStatus() != STATUS_CANCELED && order.getStatus() < 5) {
            // 已支付（待发货/运输中/已完成）——回调重复到达，幂等返回
            return;
        }
        if (order.getStatus() != STATUS_UNPAID) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR);
        }
        Date now = new Date();
        if (order.getExpireTime() != null && now.after(order.getExpireTime())) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单已超时，请重新下单");
        }
        int rows = getBaseMapper().update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, STATUS_UNPAID)
                .set(Order::getStatus, STATUS_PAID)
                .set(Order::getPayType, 0)
                .set(Order::getPayTime, now));
        if (rows == 0) {
            // 并发下已被支付或超时任务取消
            Order latest = getByOrderNo(orderNo);
            if (latest != null && latest.getStatus() == STATUS_CANCELED) {
                throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单已取消");
            }
            return;
        }
        writeStatusLog(orderNo, STATUS_UNPAID, STATUS_PAID, 1, order.getUserId(), "模拟支付成功");
        // 支付成功后清理购物车对应项
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, orderNo));
        List<Long> skuIds = items.stream().map(OrderItem::getSkuId).distinct().collect(Collectors.toList());
        cartService.removeBySkuIds(order.getUserId(), skuIds);
    }

    @Override
    public OrderVo getMineOrderVo(String orderNo) {
        return toOrderVo(getOwnedOrder(orderNo));
    }

    @Override
    public Page<OrderVo> selectMineByPage(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = SecurityUtils.getUserId();
        Page<Order> orderPage = page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getId));
        return toVoPage(orderPage);
    }

    @Override
    public int cancelExpiredOrders() {
        Date now = new Date();
        List<Order> expired = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, STATUS_UNPAID)
                .lt(Order::getExpireTime, now));
        int count = 0;
        for (Order order : expired) {
            // 条件更新互斥：与支付并发时只有一个能成功
            int rows = getBaseMapper().update(null, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getOrderNo, order.getOrderNo())
                    .eq(Order::getStatus, STATUS_UNPAID)
                    .set(Order::getStatus, STATUS_CANCELED)
                    .set(Order::getCancelTime, now));
            if (rows == 0) {
                continue;
            }
            releaseOrderResources(order);
            writeStatusLog(order.getOrderNo(), STATUS_UNPAID, STATUS_CANCELED, 3, null, "超时未支付，自动取消");
            count++;
            log.info("订单超时取消：{}", order.getOrderNo());
        }
        return count;
    }

    // ---------- P4：状态机 + 发货/收货/退款 ----------

    /** 状态机：合法流转表（只允许图中流转，非法一律拒绝） */
    private static final Map<Integer, Set<Integer>> LEGAL_TRANSITIONS = new HashMap<>();

    static {
        LEGAL_TRANSITIONS.put(0, new HashSet<>(Arrays.asList(1, 4)));   // 待支付→待发货/已取消
        LEGAL_TRANSITIONS.put(1, new HashSet<>(Arrays.asList(2, 5)));   // 待发货→运输中/退款中
        LEGAL_TRANSITIONS.put(2, new HashSet<>(Arrays.asList(3)));      // 运输中→已完成
        LEGAL_TRANSITIONS.put(5, new HashSet<>(Arrays.asList(6, 1)));   // 退款中→已退款/待发货(驳回)
        LEGAL_TRANSITIONS.put(3, Collections.emptySet());               // 已完成：不可再流转（只能售后）
        LEGAL_TRANSITIONS.put(4, Collections.emptySet());
        LEGAL_TRANSITIONS.put(6, Collections.emptySet());
    }

    private void checkTransition(int from, int to) {
        Set<Integer> allowed = LEGAL_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR,
                    "当前状态（" + OrderVo.statusName(from) + "）不允许该操作");
        }
    }

    /** 条件状态流转（互斥更新，返回行数） */
    private int doUpdate(String orderNo, int from, int to, LambdaUpdateWrapper<Order> wrapper) {
        return getBaseMapper().update(null, wrapper
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, from)
                .set(Order::getStatus, to));
    }

    @Override
    public OrderDetailVo getMineOrderDetail(String orderNo) {
        return buildDetail(getOwnedOrder(orderNo));
    }

    @Override
    public OrderDetailVo getManageOrderDetail(String orderNo) {
        return buildDetail(getByOrderNoOrThrow(orderNo));
    }

    private OrderDetailVo buildDetail(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, order.getOrderNo()).orderByAsc(OrderItem::getId));
        List<OrderStatusLog> logs = statusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderNo, order.getOrderNo()).orderByAsc(OrderStatusLog::getId));
        Logistics logistics = logisticsService.getTrace(order.getOrderNo());
        List<Refund> refunds = refundMapper.selectList(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderNo, order.getOrderNo()).orderByDesc(Refund::getId));
        Refund refund = refunds.isEmpty() ? null : refunds.get(0);
        return new OrderDetailVo(order, items, OrderVo.statusName(order.getStatus()), logs, logistics, refund);
    }

    @Override
    @Transactional
    public void cancelByUser(String orderNo) {
        Order order = getOwnedOrder(orderNo);
        checkTransition(order.getStatus(), STATUS_CANCELED);
        int rows = doUpdate(orderNo, STATUS_UNPAID, STATUS_CANCELED,
                new LambdaUpdateWrapper<Order>().set(Order::getCancelTime, new Date()));
        if (rows == 0) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单状态已变化，请刷新");
        }
        releaseOrderResources(order);
        writeStatusLog(orderNo, STATUS_UNPAID, STATUS_CANCELED, 1, order.getUserId(), "用户取消订单");
    }

    @Override
    @Transactional
    public void ship(String orderNo, ShipDto shipDto) {
        Order order = getByOrderNoOrThrow(orderNo);
        if (shipDto == null || !StringUtils.hasText(shipDto.getCompany()) || !StringUtils.hasText(shipDto.getTrackingNo())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "请填写物流公司和运单号");
        }
        checkTransition(order.getStatus(), STATUS_SHIPPED);
        int rows = doUpdate(orderNo, STATUS_PAID, STATUS_SHIPPED,
                new LambdaUpdateWrapper<Order>().set(Order::getShipTime, new Date()));
        if (rows == 0) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单状态已变化，请刷新");
        }
        logisticsService.createForOrder(order, shipDto.getCompany(), shipDto.getTrackingNo());
        writeStatusLog(orderNo, STATUS_PAID, STATUS_SHIPPED, 2, SecurityUtils.getUserId(),
                "商家发货：" + shipDto.getCompany());
    }

    @Override
    public void confirmReceive(String orderNo) {
        Order order = getOwnedOrder(orderNo);
        checkTransition(order.getStatus(), STATUS_FINISHED);
        int rows = doUpdate(orderNo, STATUS_SHIPPED, STATUS_FINISHED,
                new LambdaUpdateWrapper<Order>().set(Order::getReceiveTime, new Date()));
        if (rows == 0) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单状态已变化，请刷新");
        }
        writeStatusLog(orderNo, STATUS_SHIPPED, STATUS_FINISHED, 1, order.getUserId(), "确认收货");
    }

    @Override
    @Transactional
    public void applyRefund(String orderNo, String reason) {
        Order order = getOwnedOrder(orderNo);
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "请填写退款原因");
        }
        checkTransition(order.getStatus(), STATUS_REFUNDING);
        int rows = doUpdate(orderNo, STATUS_PAID, STATUS_REFUNDING, new LambdaUpdateWrapper<>());
        if (rows == 0) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单状态已变化，请刷新");
        }
        refundMapper.insert(new Refund()
                .setOrderNo(orderNo)
                .setUserId(order.getUserId())
                .setAmount(order.getPayAmount())
                .setReason(reason)
                .setStatus(0));
        writeStatusLog(orderNo, STATUS_PAID, STATUS_REFUNDING, 1, order.getUserId(), "申请退款：" + reason);
    }

    @Override
    @Transactional
    public void auditRefund(Long refundId, RefundAuditDto auditDto) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || refund.getStatus() != 0) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "退款单不存在或已审核");
        }
        Order order = getByOrderNoOrThrow(refund.getOrderNo());
        boolean approve = auditDto != null && Boolean.TRUE.equals(auditDto.getApprove());
        int to = approve ? STATUS_REFUNDED : STATUS_PAID;
        checkTransition(order.getStatus(), to);
        Long auditor = SecurityUtils.getUserId();
        Date now = new Date();
        int rows = doUpdate(refund.getOrderNo(), STATUS_REFUNDING, to, new LambdaUpdateWrapper<>());
        if (rows == 0) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单状态已变化，请刷新");
        }
        refund.setStatus(approve ? 1 : 2)
                .setAuditUserId(auditor)
                .setAuditTime(now)
                .setAuditRemark(auditDto == null ? null : auditDto.getRemark());
        refundMapper.updateById(refund);
        if (approve) {
            // 退款成功后回滚库存（可配置：refund_stock_rollback，默认 1）
            if (sysConfigService.getInt("refund_stock_rollback", 1) == 1) {
                List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderNo, refund.getOrderNo()));
                for (OrderItem item : items) {
                    skuMapper.restoreStock(item.getSkuId(), item.getQuantity());
                }
            }
            writeStatusLog(refund.getOrderNo(), STATUS_REFUNDING, STATUS_REFUNDED, 2, auditor, "退款审核通过，已退款");
        } else {
            writeStatusLog(refund.getOrderNo(), STATUS_REFUNDING, STATUS_PAID, 2, auditor,
                    "退款申请被驳回" + (StringUtils.hasText(refund.getAuditRemark()) ? "：" + refund.getAuditRemark() : ""));
        }
    }

    /** 释放订单资源：回补库存 + 退回优惠券（超时取消/用户取消共用） */
    private void releaseOrderResources(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, order.getOrderNo()));
        for (OrderItem item : items) {
            skuMapper.restoreStock(item.getSkuId(), item.getQuantity());
        }
        if (order.getUserCouponId() != null) {
            userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, order.getUserCouponId())
                    .eq(UserCoupon::getStatus, 1)
                    .set(UserCoupon::getStatus, 0)
                    .set(UserCoupon::getUseTime, null)
                    .set(UserCoupon::getOrderNo, null));
        }
    }

    @Override
    public Page<OrderVo> selectManageByPage(Integer pageNum, Integer pageSize, String orderNo, Integer status) {
        Page<Order> orderPage = page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<Order>()
                        .like(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getId));
        return toVoPage(orderPage);
    }

    /** 分页订单批量补明细（避免逐单查询） */
    private Page<OrderVo> toVoPage(Page<Order> orderPage) {
        List<Long> orderIds = orderPage.getRecords().stream().map(Order::getId).collect(Collectors.toList());
        Map<String, List<OrderItem>> itemMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderItem> allItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds).orderByAsc(OrderItem::getId));
            itemMap = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderNo));
        }
        List<OrderVo> vos = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            vos.add(new OrderVo(order, itemMap.getOrDefault(order.getOrderNo(), new ArrayList<>()),
                    OrderVo.statusName(order.getStatus())));
        }
        Page<OrderVo> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    private Order getByOrderNoOrThrow(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_NOT_EXIST);
        }
        return order;
    }

    // ---------- 私有工具 ----------

    private BigDecimal calcDiscount(Coupon coupon, BigDecimal total) {
        BigDecimal discount;
        if (coupon.getType() != null && coupon.getType() == 1 && coupon.getDiscountRate() != null) {
            // 折扣券：discount = total * (1 - rate)
            discount = total.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
        } else {
            discount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        }
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        // 优惠不超过商品总额
        return discount.min(total);
    }

    private String genOrderNo() {
        return "EC" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String buildFullAddress(Address address) {
        return nullToEmpty(address.getProvince()) + nullToEmpty(address.getCity())
                + nullToEmpty(address.getDistrict()) + nullToEmpty(address.getDetail());
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Order getOwnedOrder(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_NOT_EXIST);
        }
        return order;
    }

    private Order getByOrderNo(String orderNo) {
        return lambdaQuery().eq(Order::getOrderNo, orderNo).one();
    }

    private void writeStatusLog(String orderNo, int from, int to, int operatorType, Long operatorId, String remark) {
        statusLogMapper.insert(new OrderStatusLog()
                .setOrderNo(orderNo)
                .setFromStatus(from)
                .setToStatus(to)
                .setOperatorType(operatorType)
                .setOperatorId(operatorId)
                .setRemark(remark));
    }

    private OrderVo toOrderVo(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, order.getOrderNo()).orderByAsc(OrderItem::getId));
        return new OrderVo(order, items, OrderVo.statusName(order.getStatus()));
    }
}
