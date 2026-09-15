package cn.ecom.controller;

import cn.ecom.domain.dto.OrderCreateDto;
import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.vo.OrderVo;
import cn.ecom.service.OrderService;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单：下单 / 模拟支付 / 支付回调 / 状态查询 / 我的订单
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
}
