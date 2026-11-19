package com.school.lostfound.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * <p>解决「RedisTemplate 默认使用 JDK 序列化导致 Redis 中存的是乱码字节、
 * 不便调试且跨语言不兼容」的问题。本配置改为：key 用字符串序列化（可读），
 * value 用 JSON 序列化（可读、通用），例如缓存登录 Token 白名单、
 * 图像搜索结果等场景。配置后 Redis 中存储的数据形如
 * key="login:token:xxx"，value={"userId":1,...}，可用 redis-cli 直接查看。</p>
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate，替换默认序列化方式
     *
     * <p>Service 层注入 RedisTemplate<String, Object> 时会使用此 Bean。</p>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置 Redis 连接工厂（连接信息来自 application.yml 的 spring.redis 配置）
        template.setConnectionFactory(connectionFactory);

        // key 用字符串序列化：保证 Redis 中 key 是可读的明文（如 "item:1001"）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // value 用 JSON 序列化：对象存为 JSON 字符串，取出时自动反序列化回对象
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // 普通key-value 采用上述策略
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        // hash 结构的 field 与 value 也采用同样策略（系统若有 hash 缓存也保持可读）
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 序列化设置完成后必须调用，使配置生效（Spring Bean 初始化钩子）
        template.afterPropertiesSet();
        return template;
    }
}
