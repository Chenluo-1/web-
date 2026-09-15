package cn.ecom.service;

import cn.ecom.domain.pojo.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysConfigService extends IService<SysConfig> {

    /** 读取字符串配置（不存在时返回默认值） */
    String getString(String key, String defaultValue);

    /** 读取整数配置（不存在或非法时返回默认值） */
    int getInt(String key, int defaultValue);
}
