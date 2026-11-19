package com.school.lostfound.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT（JSON Web Token）工具类。
 *
 * <p>功能：负责 Token 的生成、解析、校验与过期判断，是系统无状态登录认证的核心。
 * 用户登录成功后由后端签发 Token；此后每次请求，前端把 Token 放在
 * Authorization 请求头中，JwtAuthenticationFilter 调用本类解析出用户身份，
 * 无需在服务端保存 Session——这就是"无状态认证"，便于水平扩展。</p>
 *
 * <p>JWT 结构：Header.Payload.Signature 三段 Base64URL 字符串。
 * - Header：声明签名算法（本项目使用 HS256）；
 * - Payload：存放 claims（userId、username、role、签发时间 iat、过期时间 exp）；
 * - Signature：用服务端密钥对前两段做 HMAC-SHA256 签名，防止内容被篡改。</p>
 *
 * <p>为什么需要它：Spring Security 的过滤器需要一种统一、可靠的方式
 * "从请求里还原出用户是谁"，本类把 jjwt 库的 API 封装成几个语义清晰的方法，
 * 供认证过滤器与登录接口复用。</p>
 */
@Component
public class JwtUtil {

    /** 签名密钥字符串，从 application.yml 的 jwt.secret 读取。
     *  安全要求：HS256 密钥长度须 >= 32 字节（256 位），且不能泄露到前端。 */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 有效期（毫秒），从 jwt.expiration 读取，用于计算过期时间。 */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 将密钥字符串包装成 jjwt 需要的 SecretKey 对象。
     *
     * <p>逐行说明：
     * secret.getBytes(StandardCharsets.UTF_8) —— 把密钥字符串按 UTF-8 编码转为字节数组；
     * Keys.hmacShaKeyFor(...) —— 校验字节数长度是否满足 HMAC-SHA256 要求（至少 32 字节），
     * 并构建成 HMAC-SHA 的 SecretKey，用于后续 signWith / verifyWith。</p>
     *
     * <p>每次调用都新建一个 key 对象（SecretKey 本身不可变、开销很小），
     * 这样 secret 字段可以在 Spring 注入完成后再使用，避免构造函数时机问题。</p>
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token：登录成功后调用，把用户身份信息写进 Payload。
     *
     * <p>逐行说明：
     * claims —— 自定义声明：userId（唯一标识，用于过滤器还原登录用户）、
     * username（用户名）、role（角色 USER/ADMIN，用于权限判断）；
     * Jwts.builder() —— 使用建造者模式逐步构建 JWT；
     * .claims(claims) —— 写入上面的自定义 claims；
     * .subject(username) —— 设置标准声明 sub（主题），JWT 惯例用 sub 表示主体身份；
     * .issuedAt(new Date()) —— 标准声明 iat：签发时间 = 当前时间；
     * .expiration(new Date(now + expiration)) —— 标准声明 exp：过期时间 =
     * 当前时间 + 配置的有效期，jjwt 解析时会自动校验该时间；
     * .signWith(getSigningKey()) —— 用 HS256 算法和密钥对
     * "Base64URL(Header) + '.' + Base64URL(Payload)" 计算签名，生成第三段；
     * .compact() —— 把三段用 '.' 拼接成最终的 Token 字符串。</p>
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token 并返回其中的 Claims（Payload 部分）。
     *
     * <p>逐行说明：
     * Jwts.parser() —— 创建解析器建造者；
     * .verifyWith(getSigningKey()) —— 指定验签密钥：jjwt 会用同一密钥重算签名，
     * 与 Token 里的签名比对，不一致（说明内容被篡改或密钥不对）直接抛异常；
     * .build() —— 构建出解析器；
     * .parseSignedClaims(token) —— 解析"有签名的 JWT"：依次完成
     * ①拆分三段并 Base64URL 解码；②验签；③校验 exp 是否已过期，
     * 任何一步失败都会抛出对应异常（签名不符 / ExpiredJwtException / 格式错误）；
     * .getPayload() —— 取出解析后的 Payload，即 Claims 对象。</p>
     *
     * <p>注意：本方法不做 try-catch，异常向上抛给调用方，
     * 由 validateToken 等方法统一捕获处理。</p>
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中取出 userId（我们自定义的 claim）。
     * claims.get("userId", Long.class) 表示按 Long 类型读取，类型不符会抛异常。
     * JwtAuthenticationFilter 主要靠这个方法还原当前登录用户。
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中取出用户名，即标准声明 sub（生成时的 subject）。
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中取出角色（USER / ADMIN），用于 Spring Security 的权限判断。
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 判断 Token 是否已过期。
     *
     * <p>逻辑：取出 exp 声明的时间，若早于当前时间则已过期。
     * catch 兜底：如果 Token 本身就解析失败（伪造、格式错误），
     * 视同"过期/无效"，返回 true，避免异常外泄。</p>
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 综合校验 Token 是否有效，认证过滤器的入口方法。
     *
     * <p>校验通过需同时满足两个条件：
     * ① parseToken 成功 —— 说明签名正确（未被篡改）且格式合法、未超过 exp（jjwt 自带过期校验）；
     * ② isTokenExpired 为 false —— 双重保险，显式再比一次过期时间。</p>
     *
     * <p>任何异常（SignatureException、ExpiredJwtException、MalformedJwtException 等）
     * 都返回 false，调用方据此拒绝本次请求（返回 401）。</p>
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
