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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/found-items", "/api/found-items/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lost-notices", "/api/lost-notices/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/drop-points", "/api/drop-points/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/config").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // 留档库：站点管理员可查本点位留档，系统管理员可查全部
                        .requestMatchers("/api/admin/archive/**").hasAnyRole("SYS_ADMIN","POINT_ADMIN")
                        // 站点管理员可下架本点位物品（方法内已校验点位归属）
                        .requestMatchers(HttpMethod.PUT, "/api/admin/items/*/takedown").hasAnyRole("SYS_ADMIN","POINT_ADMIN")
                        // 站点管理员可为本点位发起监控调取申请（审批仍归系统管理员）
                        .requestMatchers(HttpMethod.POST, "/api/admin/camera-logs").hasAnyRole("SYS_ADMIN","POINT_ADMIN")
                        // 站点管理员可查看本点位调取申请与审批进度
                        .requestMatchers(HttpMethod.GET, "/api/admin/camera-logs/mine").hasAnyRole("SYS_ADMIN","POINT_ADMIN")
                        // 其余系统级接口（用户、积分、风控、监控调取）仅系统管理员
                        .requestMatchers("/api/admin/**").hasRole("SYS_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
