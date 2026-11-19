package com.school.lostfound.config;

import com.school.lostfound.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 安全配置类
 *
 * <p>本系统采用「前后端分离 + JWT 无状态认证」方案，因此该配置解决以下几个核心问题：</p>
 * <ol>
 *   <li>关闭 CSRF 与 Session：JWT 方式下服务端不保存会话，无需 CSRF 防护和 Session</li>
 *   <li>配置 URL 级别的访问规则：哪些接口可以匿名访问（如登录、注册、公开查询），
 *       哪些只有管理员角色才能访问，其余一律要求登录</li>
 *   <li>注册 JWT 过滤器：在每个请求进入业务逻辑前解析 Token 并完成认证</li>
 *   <li>提供 BCrypt 密码加密器：数据库中绝不存明文密码</li>
 *   <li>配置 CORS：允许前端（不同端口/域名）跨域调用后端 API</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity          // 开启 Spring Security 的 Web 安全支持
@EnableMethodSecurity       // 开启方法级权限注解（如 @PreAuthorize("hasRole('SYS_ADMIN')")）
@RequiredArgsConstructor
public class SecurityConfig {

    /** 自定义 JWT 认证过滤器：从请求头解析 Token 并把用户信息塞进 SecurityContext */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码加密器（BCrypt 算法）
     *
     * <p>解决「明文密码存储」问题：用户注册时用 encode() 加密入库，
     * 登录时用 matches() 比对。BCrypt 自带随机盐，同一密码每次加密结果都不同，
     * 可抵御彩虹表攻击。</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     *
     * <p>从 Spring Security 已配置好的 AuthenticationConfiguration 中取出 AuthenticationManager，
     * 供登录接口（AuthController）调用，用于校验「用户名 + 密码」是否正确。</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 安全过滤链配置（核心）
     *
     * <p>定义每个 HTTP 请求经过的规则：CSRF/CORS/Session 策略、哪些路径放行、
     * 剩余请求是否需要认证，以及把 JWT 过滤器插到用户名密码过滤器之前。</p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF 防护：无状态 JWT 认证下没有 Session Cookie 可被伪造，此配置是前后端分离的标准做法
                .csrf(AbstractHttpConfigurer::disable)
                // 启用 CORS，使用下方定义的跨域配置，解决前端跨域请求被浏览器拦截的问题
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Session 策略设为 STATELESS（无状态）：服务端不创建/使用 HttpSession，每次请求都靠 JWT 识别用户
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // URL 级别的权限规则（按顺序匹配，命中即停）
                .authorizeHttpRequests(auth -> auth
                        // 注册、登录接口对所有人开放（未登录也要能用）
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        // 失物招领的公开浏览类 GET 接口放行：游客也能查看招领、寻物、站点列表
                        .requestMatchers(HttpMethod.GET, "/api/found-items", "/api/found-items/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lost-notices", "/api/lost-notices/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/drop-points", "/api/drop-points/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/config").permitAll()
                        // 上传的图片等静态资源直接放行，页面上 <img> 标签无需携带 Token
                        .requestMatchers("/uploads/**").permitAll()
                        // 管理端接口只允许「系统管理员」或「站点管理员」角色访问
                        .requestMatchers("/api/admin/**").hasAnyRole("SYS_ADMIN","POINT_ADMIN")
                        // 除上述外的所有请求都需要登录（携带有效 JWT）
                        .anyRequest().authenticated()
                )
                // 把 JWT 过滤器插入到 Spring Security 内置的用户名密码认证过滤器之前，
                // 保证请求先经过 Token 解析认证，再走后续的授权判断
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 跨域配置
     *
     * <p>解决「前端(如 localhost:5173)调用后端(如 localhost:8080)时浏览器同源策略拦截」的问题。
     * 允许任意来源（开发环境方便）、常用 HTTP 方法与请求头，并允许携带 Cookie/凭证；
     * 预检请求(OPTIONS)结果缓存 1 小时，减少重复预检请求。</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的来源模式：* 表示任意 Origin（生产环境建议改成前端具体域名）
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许携带的请求头
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        // 允许携带凭证（Cookie、Authorization 头）
        configuration.setAllowCredentials(true);
        // 预检请求的缓存时间（秒），3600 = 1 小时内浏览器不再重复发送 OPTIONS 预检
        configuration.setMaxAge(3600L);

        // 把以上规则注册到所有路径（/**）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
