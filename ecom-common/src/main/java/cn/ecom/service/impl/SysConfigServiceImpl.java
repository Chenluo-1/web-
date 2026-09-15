package cn.ecom.service.impl;

import cn.ecom.domain.pojo.SysConfig;
import cn.ecom.mapper.SysConfigMapper;
import cn.ecom.service.SysConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 系统参数服务：所有规则阈值从此读取，禁止写死代码
 */
@Service("sysConfigService")
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getString(String key, String defaultValue) {
        SysConfig config = lambdaQuery().eq(SysConfig::getConfigKey, key).one();
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
