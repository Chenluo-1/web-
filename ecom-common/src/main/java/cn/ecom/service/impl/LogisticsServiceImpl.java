package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Logistics;
import cn.ecom.domain.pojo.Order;
import cn.ecom.mapper.LogisticsMapper;
import cn.ecom.service.LogisticsService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 物流服务：模拟轨迹按"发货后时间点"虚拟推进。
 * 演示节奏：0 分钟已揽收、2 分钟运输中、4 分钟派送中、6 分钟已签收（读取时推进并落库）。
 */
@Service("logisticsService")
public class LogisticsServiceImpl extends ServiceImpl<LogisticsMapper, Logistics> implements LogisticsService {

    /** 各阶段相对发货的分钟偏移 */
    private static final int[] STAGE_MINUTES = {0, 2, 4, 6};

    @Override
    public Logistics createForOrder(Order order, String company, String trackingNo) {
        Date now = new Date();
        JSONArray trace = new JSONArray();
        trace.add(traceItem(now, company + "：包裹已揽收"));
        Logistics logistics = new Logistics()
                .setOrderNo(order.getOrderNo())
                .setCompany(company)
                .setTrackingNo(trackingNo)
                .setStatus(0)
                .setTraceJson(trace.toJSONString())
                .setCreateTime(now);
        save(logistics);
        return logistics;
    }

    @Override
    public Logistics getTrace(String orderNo) {
        Logistics logistics = lambdaQuery().eq(Logistics::getOrderNo, orderNo).one();
        if (logistics == null) {
            return null;
        }
        return refreshProgress(logistics);
    }

    /** 读取时按已过去分钟数推进物流阶段，新阶段追加轨迹并落库 */
    private Logistics refreshProgress(Logistics logistics) {
        if (logistics.getStatus() == null || logistics.getStatus() >= 3 || logistics.getCreateTime() == null) {
            return logistics;
        }
        long minutes = (System.currentTimeMillis() - logistics.getCreateTime().getTime()) / 60_000L;
        int target = 0;
        for (int stage = 0; stage < STAGE_MINUTES.length; stage++) {
            if (minutes >= STAGE_MINUTES[stage]) {
                target = stage;
            }
        }
        if (target <= logistics.getStatus()) {
            return logistics;
        }
        JSONArray trace = JSON.parseArray(logistics.getTraceJson());
        if (trace == null) {
            trace = new JSONArray();
        }
        Date now = new Date();
        for (int stage = logistics.getStatus() + 1; stage <= target; stage++) {
            trace.add(traceItem(now, stageText(stage, logistics)));
        }
        logistics.setStatus(target).setTraceJson(trace.toJSONString());
        updateById(logistics);
        return logistics;
    }

    private String stageText(int stage, Logistics logistics) {
        switch (stage) {
            case 1: return logistics.getCompany() + "：包裹运输中";
            case 2: return logistics.getCompany() + "：包裹派送中";
            case 3: return "包裹已签收，感谢使用";
            default: return "包裹已揽收";
        }
    }

    private JSONObject traceItem(Date time, String text) {
        JSONObject item = new JSONObject();
        item.put("time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time));
        item.put("text", text);
        return item;
    }
}
