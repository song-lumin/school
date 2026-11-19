package com.school.lostfound.security;

import com.school.lostfound.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器 —— 每个HTTP请求进入 Controller 之前的"身份检查哨"。
 * <p>
 * 业务背景：本系统采用 JWT 无状态认证，没有登录表单跳转和服务器端 Session。
 * 前端每次调用受保护接口时，在请求头 Authorization 中携带 "Bearer <token>"；
 * Spring Security 的过滤链在业务逻辑执行前，需要一个环节把令牌"翻译"成
 * SecurityContext 中的 Authentication 对象，后续的权限注解（如 hasRole）
 * 和代码中获取当前用户 ID 的逻辑都依赖它。本类就是这个翻译环节。
 * <p>
 * 继承 OncePerRequestFilter 保证同一请求只执行一次过滤逻辑（防止 forward/include
 * 时重复执行）。注意：本过滤器只负责"验明身份"，不做"授权决策"——无效令牌时
 * 不抛错也不中断，而是不设置认证信息继续放行，最终由 SecurityConfig 的授权规则
 * 决定该匿名请求是否被允许（公开接口放行，受保护接口返回 401）。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，负责验签与解析（构造器注入，由 Lombok 生成） */
    private final JwtUtil jwtUtil;

    /**
     * 过滤器核心逻辑：从请求中提取 JWT → 验证 → 构建认证对象写入 SecurityContext。
     *
     * @param request     当前 HTTP 请求，用于读取 Authorization 请求头
     * @param response    响应对象，本过滤器不直接写响应
     * @param filterChain 过滤器链，处理完后必须调用 doFilter 让请求继续向后传递
     *
     * 处理流程：
     * 1. 请求头无令牌（如匿名访问公告接口）→ 直接放行，交给后续授权规则判断；
     * 2. 令牌验签失败/过期 → 同样不设置认证信息，放行后由授权规则返回 401；
     * 3. 令牌有效 → 解析出 userId/username/role，构造 Authentication 放入
     *    SecurityContextHolder，之后本次请求内的所有代码都能拿到当前用户身份。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 从 Authorization 请求头提取 Bearer 令牌；没有则返回 null
        String token = extractToken(request);

        // 仅当令牌存在且验签/有效期校验都通过时才建立认证上下文
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            // 从 payload 中取出签发时写入的三类身份信息
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            // 构造已认证（authenticated）状态的 Authentication 对象：
            // principal 放 userId（系统约定：业务代码直接拿到的是用户主键而非 User 对象），
            // credentials 传 null（JWT 无状态认证，登录凭证只在登录时使用一次），
            // authorities 填 "ROLE_" 前缀 + 角色，对齐 Spring Security 的 hasRole('XXX') 规则
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
            // 附加请求细节（客户端 IP、sessionId 等），便于审计日志与安全策略使用
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 写入安全上下文 —— 这是"当前登录用户"在本请求内的唯一数据来源
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 无论认证与否都继续过滤链；受保护接口由后续授权规则拦截并返回 401/403
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 JWT 令牌。
     *
     * @param request 当前 HTTP 请求
     * @return 令牌字符串；请求头缺失或格式不为 "Bearer xxx" 时返回 null
     *
     * 规范说明：HTTP 官方认证方案是 "Authorization: Bearer <token>"，
     * 前缀 "Bearer "（含空格）固定 7 个字符，截掉即得纯令牌。
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // hasText 同时排除 null、空串、纯空白；startsWith 保证格式符合 Bearer 方案
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 去掉 "Bearer " 前缀，只留令牌本体
        }
        return null; // 无令牌：按匿名请求处理，交给授权规则裁决
    }
}
