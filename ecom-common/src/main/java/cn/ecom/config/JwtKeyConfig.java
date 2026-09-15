package cn.ecom.config;

import cn.ecom.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动时将 application.yml 的 jwt.key 注入 JwtUtil（避免密钥硬编码在代码里）
 */
@Component
public class JwtKeyConfig {

    @Value("${jwt.key:ecom}")
    public void setKey(String key) {
        JwtUtil.setJwtKey(key);
    }
}
