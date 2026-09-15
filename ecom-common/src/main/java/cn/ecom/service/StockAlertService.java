package cn.ecom.service;

import cn.ecom.domain.pojo.StockAlert;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface StockAlertService extends IService<StockAlert> {

    /** 扫描全部 SKU 生成预警（阈值走 ec_sys_config；同 SKU 同类型未处理时防重复），返回新生成数量 */
    int scanAndCreateAlerts();

    Page<StockAlert> selectByPage(Integer pageNum, Integer pageSize, Integer alertType, Integer status);

    /** 标记已处理 */
    void handle(Long id);

    /** 未处理预警数（首页卡片） */
    long unhandledCount();
}
