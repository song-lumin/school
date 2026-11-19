package com.school.lostfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园失物招领系统 - 后端启动入口类
 *
 * <p>这是整个 Spring Boot 应用的启动类，main 方法是程序的唯一入口。
 * 运行该类即可启动内嵌 Tomcat 服务器，加载所有 Controller、Service、Mapper 等组件，
 * 对外提供失物招领相关的 REST API 服务。</p>
 *
 * <p>三个注解的作用：</p>
 * <ul>
 *   <li>{@code @SpringBootApplication}：组合注解，开启自动配置（@EnableAutoConfiguration）、
 *       组件扫描（@ComponentScan，默认扫描本类所在包及其子包）和 Spring Boot 应用标识</li>
 *   <li>{@code @MapperScan("com.school.lostfound.mapper")}：MyBatis-Plus 提供的注解，
 *       指定扫描 mapper 包下的所有接口并为它们生成代理实现，注册为 Spring Bean，
 *       这样 Service 层才能直接注入各 Mapper 操作数据库</li>
 *   <li>{@code @EnableScheduling}：开启 Spring 的定时任务功能，
 *       使 schedule 包下的 @Scheduled 定时任务（如物品过期、到期提醒）能自动执行</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.school.lostfound.mapper")
@EnableScheduling
public class LostFoundApplication {

    /**
     * 程序入口方法，委托给 SpringApplication 启动整个应用（内嵌 Web 服务器 + IoC 容器）
     */
    public static void main(String[] args) {
        SpringApplication.run(LostFoundApplication.class, args);
    }
}
