package cn.ecom.service;

import cn.ecom.domain.dto.OrderCreateDto;
import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.vo.OrderVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderService extends IService<Order> {

    /** 下单：校验→原子扣库存→核销券→快照明细→幂等键，返回订单号与支付参数 */
    OrderVo createOrder(OrderCreateDto dto);

    /** 按幂等键查已有订单（重复提交时返回同一笔） */
    Order getByUserIdempotencyKey(Long userId, String idempotencyKey);

    /** 模拟支付（幂等：已支付则直接返回成功） */
    void mockPay(String orderNo);

    /** 支付回调（模拟网关，幂等） */
    String payCallback(String orderNo);

    /** 订单详情（仅本人；返回状态文案） */
    OrderVo getMineOrderVo(String orderNo);

    /** 我的订单分页（按状态筛选） */
    Page<OrderVo> selectMineByPage(Integer pageNum, Integer pageSize, Integer status);

    /** 扫描并取消超时未支付订单（释放库存、退回优惠券），返回处理数量 */
    int cancelExpiredOrders();
}
