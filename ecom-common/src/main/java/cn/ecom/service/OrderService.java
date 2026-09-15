package cn.ecom.service;

import cn.ecom.domain.dto.OrderCreateDto;
import cn.ecom.domain.dto.RefundAuditDto;
import cn.ecom.domain.dto.ShipDto;
import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.vo.OrderDetailVo;
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

    // ---------- P4：状态机与售后 ----------

    /** 订单详情（本人）：状态时间轴 + 物流 + 退款单 */
    OrderDetailVo getMineOrderDetail(String orderNo);

    /** 用户取消待支付订单（释放库存、退回优惠券） */
    void cancelByUser(String orderNo);

    /** 发货（管理员）：待发货→运输中，创建物流单 */
    void ship(String orderNo, ShipDto shipDto);

    /** 确认收货（本人）：运输中→已完成 */
    void confirmReceive(String orderNo);

    /** 申请退款（本人）：待发货→退款中，创建退款单 */
    void applyRefund(String orderNo, String reason);

    /** 审核退款（管理员）：通过→已退款（按配置回滚库存）；驳回→回待发货 */
    void auditRefund(Long refundId, RefundAuditDto auditDto);

    /** 后台订单分页（全部订单，按订单号/状态筛选） */
    Page<OrderVo> selectManageByPage(Integer pageNum, Integer pageSize, String orderNo, Integer status);

    /** 后台订单详情 */
    OrderDetailVo getManageOrderDetail(String orderNo);
}
