package cn.ecom.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JWT 工具测试（回归保护）：
 * jwt.key 可能被配置成 Base64 串，也可能是随机串——两种都必须能用。
 * 修复前 generalKey() 无条件 Base64 解码，配随机串会在每次鉴权时抛
 * IllegalArgumentException（启动不报错、一登录就 500）。
 */
class JwtUtilTest {

    /** 随机串密钥（含 - 和 _，不是合法 Base64）：修复前这里直接崩 */
    @Test
    void parseTokenWithNonBase64Key() throws Exception {
        JwtUtil.setJwtKey("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        String token = JwtUtil.createJWT("42");
        Claims claims = JwtUtil.parseJWT(token);
        assertEquals("42", claims.getSubject());
    }

    /** 纯字母数字但长度不是 4 的倍数（同样不是合法 Base64） */
    @Test
    void parseTokenWithOddLengthKey() throws Exception {
        JwtUtil.setJwtKey("ecom-secret-2026");
        String token = JwtUtil.createJWT("7");
        assertEquals("7", JwtUtil.parseJWT(token).getSubject());
    }

    /** 合法 Base64 密钥（如项目默认的 ecom）走解码分支，行为与修复前一致 */
    @Test
    void parseTokenWithBase64Key() throws Exception {
        JwtUtil.setJwtKey("ecom");
        String token = JwtUtil.createJWT("1");
        assertEquals("1", JwtUtil.parseJWT(token).getSubject());
    }

    /** 密钥变更后缓存失效：老 token 不能用新密钥解析通过 */
    @Test
    void keyChangeInvalidatesCachedKey() {
        JwtUtil.setJwtKey("ecom");
        String token = JwtUtil.createJWT("9");
        JwtUtil.setJwtKey("another-key-2026");
        boolean rejected = false;
        try {
            JwtUtil.parseJWT(token);
        } catch (Exception e) {
            rejected = true;
        }
        assertEquals(true, rejected, "换密钥后旧 token 必须解析失败");
    }
}
