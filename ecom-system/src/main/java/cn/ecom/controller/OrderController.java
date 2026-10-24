package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.dto.OrderCreateDto;
import cn.ecom.domain.dto.RefundApplyDto;
import cn.ecom.domain.dto.RefundAuditDto;
import cn.ecom.domain.dto.ShipDto;
import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.vo.OrderDetailVo;
import cn.ecom.domain.vo.OrderVo;
import cn.ecom.service.OrderService;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单：下单 / 模拟支付 / 状态查询 / 我的订单 / 售后（取消、收货、退款） / 后台管理（发货、退款审核）
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 下单（同一幂等键重复提交返回同一笔订单） */
    @PostMapping
    public ResponseResult<OrderVo> create(@RequestBody OrderCreateDto dto) {
        try {
            return ResponseResult.okResult(orderService.createOrder(dto));
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一索引兜底，事务已回滚，返回已存在订单
            Long userId = SecurityUtils.getUserId();
            Order exist = orderService.getByUserIdempotencyKey(userId, dto.getIdempotencyKey());
            if (exist != null) {
                return ResponseResult.okResult(orderService.getMineOrderVo(exist.getOrderNo()));
            }
            throw e;
        }
    }

    /** 模拟支付 */
    @PostMapping("/{orderNo}/pay")
    public ResponseResult<Void> pay(@PathVariable String orderNo) {
        orderService.mockPay(orderNo);
        return ResponseResult.okResult();
    }

    /** 支付回调（模拟网关回调，幂等：重复调用只生效一次） */
    @PostMapping("/pay/callback")
    public ResponseResult<String> callback(@RequestParam String orderNo) {
        return ResponseResult.okResult(orderService.payCallback(orderNo));
    }

    /** 主动查询支付结果/订单状态 */
    @GetMapping("/{orderNo}/status")
    public ResponseResult<OrderVo> status(@PathVariable String orderNo) {
        return ResponseResult.okResult(orderService.getMineOrderVo(orderNo));
    }

    /** 我的订单（按状态筛选） */
    @GetMapping("/mine")
    public ResponseResult<Page<OrderVo>> mine(Integer pageNum, Integer pageSize, Integer status) {
        return ResponseResult.okResult(orderService.selectMineByPage(pageNum, pageSize, status));
    }

    // ---------- 售后（消费者） ----------

    /** 订单详情（状态时间轴 + 物流 + 退款单） */
    @GetMapping("/{orderNo}/detail")
    public ResponseResult<OrderDetailVo> detail(@PathVariable String orderNo) {
        return ResponseResult.okResult(orderService.getMineOrderDetail(orderNo));
    }

    /** 取消订单（仅待支付） */
    @OperLog(module = "订单", type = "取消订单")
    @PostMapping("/{orderNo}/cancel")
    public ResponseResult<Void> cancel(@PathVariable String orderNo) {
        orderService.cancelByUser(orderNo);
        return ResponseResult.okResult();
    }

    /** 确认收货（仅运输中） */
    @OperLog(module = "订单", type = "确认收货")
    @PostMapping("/{orderNo}/receive")
    public ResponseResult<Void> receive(@PathVariable String orderNo) {
        orderService.confirmReceive(orderNo);
        return ResponseResult.okResult();
    }

    /** 申请退款（仅待发货） */
    @OperLog(module = "订单", type = "申请退款")
    @PostMapping("/{orderNo}/refund")
    public ResponseResult<Void> applyRefund(@PathVariable String orderNo, @RequestBody RefundApplyDto dto) {
        orderService.applyRefund(orderNo, dto == null ? null : dto.getReason());
        return ResponseResult.okResult();
    }

    // ---------- 管理端（运营） ----------

    /** 后台订单分页 */
    @GetMapping("/manage")
    @PreAuthorize("@ss.hasPermission('order:manage')")
    public ResponseResult<Page<OrderVo>> manage(Integer pageNum, Integer pageSize,
                                                String orderNo, Integer status) {
        return ResponseResult.okResult(orderService.selectManageByPage(pageNum, pageSize, orderNo, status));
    }

    /** 后台订单详情 */
    @GetMapping("/manage/{orderNo}")
    @PreAuthorize("@ss.hasPermission('order:manage')")
    public ResponseResult<OrderDetailVo> manageDetail(@PathVariable String orderNo) {
        return ResponseResult.okResult(orderService.getManageOrderDetail(orderNo));
    }

    /** 发货（待发货→运输中） */
    @OperLog(module = "订单管理", type = "发货")
    @PreAuthorize("@ss.hasPermission('order:ship')")
    @PostMapping("/{orderNo}/ship")
    public ResponseResult<Void> ship(@PathVariable String orderNo, @RequestBody ShipDto dto) {
        orderService.ship(orderNo, dto);
        return ResponseResult.okResult();
    }

    /** 审核退款（通过→已退款并按配置回滚库存；驳回→回待发货） */
    @OperLog(module = "订单管理", type = "审核退款")
    @PreAuthorize("@ss.hasPermission('order:refund')")
    @PutMapping("/refund/{refundId}/audit")
    public ResponseResult<Void> auditRefund(@PathVariable Long refundId, @RequestBody RefundAuditDto dto) {
        orderService.auditRefund(refundId, dto);
        return ResponseResult.okResult();
    }
}
