package com.school.lostfound.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * <p>解决「分页查询不生效」的问题。MyBatis-Plus 自带的分页（Page 对象）需要
 * 配置分页拦截器才能把 Page 参数转写为数据库的 LIMIT 语句；
 * 若不配置此 Bean，分页查询会查出全表数据而非指定页。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链，加入 MySQL 方言的分页内部拦截器
     *
     * <p>分页拦截器会在 SQL 执行前自动拼接 LIMIT 子句，并额外执行一条
     * COUNT 语句计算总数，从而支撑 Controller/Service 中 IPage 分页结果的返回。</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 指定数据库类型为 MySQL，拦截器会按 MySQL 的 LIMIT 语法生成分页 SQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
