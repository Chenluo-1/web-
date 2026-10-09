package cn.ecom.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类（照 seig）
 */
public class JwtUtil {

    /** 有效期 24 小时 */
    public static final Long JWT_TTL = 24 * 60 * 60 * 1000L;
    /** 密钥明文（合法 Base64 按 Base64 解码，否则按 UTF-8 字节），启动时由 JwtKeyConfig 从 application.yml 的 jwt.key 注入 */
    private static String JWT_KEY = "ecom";
    /** 解码后的密钥缓存（避免每次校验 token 都重新解码） */
    private static volatile SecretKey cachedKey;

    /** 由 Spring 启动阶段注入配置密钥；密钥变更后缓存失效 */
    public static void setJwtKey(String key) {
        if (key != null && !key.trim().isEmpty()) {
            JWT_KEY = key.trim();
            cachedKey = null;
        }
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /** 生成 JWT（subject 一般为用户 id） */
    public static String createJWT(String subject) {
        return getJwtBuilder(subject, null, getUUID()).compact();
    }

    public static String createJWT(String subject, Long ttlMillis) {
        return getJwtBuilder(subject, ttlMillis, getUUID()).compact();
    }

    private static JwtBuilder getJwtBuilder(String subject, Long ttlMillis, String uuid) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        SecretKey secretKey = generalKey();
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis == null) {
            ttlMillis = JwtUtil.JWT_TTL;
        }
        Date expDate = new Date(nowMillis + ttlMillis);
        return Jwts.builder()
                .setId(uuid)
                .setSubject(subject)
                .setIssuer("ecom")
                .setIssuedAt(now)
                .signWith(signatureAlgorithm, secretKey)
                .setExpiration(expDate);
    }

    /** 生成加密后的秘钥 */
    public static SecretKey generalKey() {
        SecretKey key = cachedKey;
        if (key == null) {
            synchronized (JwtUtil.class) {
                key = cachedKey;
                if (key == null) {
                    byte[] encodedKey = decodeKey(JWT_KEY);
                    key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
                    cachedKey = key;
                }
            }
        }
        return key;
    }

    /**
     * 密钥取字节：是合法 Base64 就按 Base64 解码，否则退化为 UTF-8 字节。
     * 这样 jwt.key 既可以是 Base64 串也可以是任意随机串——直接 decode 会让非 Base64
     * 的密钥在每次鉴权时抛 IllegalArgumentException（启动不报错、一登录就 500）。
     */
    private static byte[] decodeKey(String raw) {
        String value = raw == null ? "" : raw;
        if (value.length() % 4 == 0 && value.matches("[A-Za-z0-9+/]+={0,2}")) {
            try {
                return Base64.getDecoder().decode(value);
            } catch (IllegalArgumentException ignored) {
                // 落入下面的 UTF-8 分支
            }
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /** 解析 token */
    public static Claims parseJWT(String jwt) throws Exception {
        SecretKey secretKey = generalKey();
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt)
                .getBody();
    }
}
