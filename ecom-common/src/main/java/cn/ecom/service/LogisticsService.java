package cn.ecom.service;

import cn.ecom.domain.pojo.Logistics;
import cn.ecom.domain.pojo.Order;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LogisticsService extends IService<Logistics> {

    /** 发货时创建物流单（含首条"已揽收"轨迹） */
    Logistics createForOrder(Order order, String company, String trackingNo);

    /** 查询物流（按发货时间虚拟推进轨迹：已揽收→运输中→派送中→已签收） */
    Logistics getTrace(String orderNo);
}
