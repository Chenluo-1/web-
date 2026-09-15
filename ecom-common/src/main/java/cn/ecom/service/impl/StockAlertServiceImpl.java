package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Sku;
import cn.ecom.domain.pojo.StockAlert;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.SkuMapper;
import cn.ecom.mapper.StockAlertMapper;
import cn.ecom.service.StockAlertService;
import cn.ecom.service.SysConfigService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 库存预警服务：阈值配置化（ec_sys_config），同 SKU 同类型未处理时只推首次
 */
@Slf4j
@Service("stockAlertService")
public class StockAlertServiceImpl extends ServiceImpl<StockAlertMapper, StockAlert> implements StockAlertService {

    /** 预警类型 */
    private static final int TYPE_LOW = 0;
    private static final int TYPE_HIGH = 1;

    private final SkuMapper skuMapper;
    private final SysConfigService sysConfigService;

    public StockAlertServiceImpl(SkuMapper skuMapper, SysConfigService sysConfigService) {
        this.skuMapper = skuMapper;
        this.sysConfigService = sysConfigService;
    }

    @Override
    @Transactional
    public int scanAndCreateAlerts() {
        int low = sysConfigService.getInt("stock_alert_low_default", 10);
        int high = sysConfigService.getInt("stock_alert_high_default", 1000);
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getStatus, 1));
        int created = 0;
        for (Sku sku : skus) {
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            Integer type = null;
            Integer threshold = null;
            if (stock < low) {
                type = TYPE_LOW;
                threshold = low;
            } else if (stock > high) {
                type = TYPE_HIGH;
                threshold = high;
            }
            if (type == null) {
                continue;
            }
            // 防重复：同 SKU 同类型存在未处理预警则跳过（只推首次）
            Integer pending = lambdaQuery()
                    .eq(StockAlert::getSkuId, sku.getId())
                    .eq(StockAlert::getAlertType, type)
                    .eq(StockAlert::getStatus, 0)
                    .count();
            if (pending != null && pending > 0) {
                continue;
            }
            save(new StockAlert()
                    .setSkuId(sku.getId())
                    .setProductId(sku.getProductId())
                    .setAlertType(type)
                    .setCurrentStock(stock)
                    .setThresholdValue(threshold)
                    .setStatus(0));
            created++;
        }
        return created;
    }

    @Override
    public Page<StockAlert> selectByPage(Integer pageNum, Integer pageSize, Integer alertType, Integer status) {
        return page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<StockAlert>()
                        .eq(alertType != null, StockAlert::getAlertType, alertType)
                        .eq(status != null, StockAlert::getStatus, status)
                        .orderByDesc(StockAlert::getId));
    }

    @Override
    public void handle(Long id) {
        StockAlert alert = getById(id);
        if (alert == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "预警记录不存在");
        }
        if (alert.getStatus() != null && alert.getStatus() == 1) {
            return;
        }
        updateById(new StockAlert()
                .setId(id)
                .setStatus(1)
                .setHandleTime(new Date())
                .setHandleUserId(SecurityUtils.getUserId()));
    }

    @Override
    public long unhandledCount() {
        Integer count = lambdaQuery().eq(StockAlert::getStatus, 0).count();
        return count == null ? 0 : count;
    }
}
