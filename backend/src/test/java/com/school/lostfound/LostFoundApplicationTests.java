package com.school.lostfound;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 上下文冒烟测试。
 *
 * <p>测试模块：整个后端应用（LostFoundApplication）。</p>
 * <p>覆盖场景：验证 Spring 容器能否正常启动——所有 Bean（Controller、Service、
 * Mapper、配置类等）能被成功装配，无循环依赖或配置错误。</p>
 * <p>说明：这是最基础的集成测试。若 contextLoads 失败，说明应用本身无法启动，
 * 其他所有测试都将失去意义。</p>
 */
@SpringBootTest
class LostFoundApplicationTests {

    /**
     * 验证 Spring 应用上下文可以成功加载。
     * 预期：@SpringBootTest 启动完整容器时不抛出任何异常即通过。
     * 为什么：能最早发现 Bean 装配、配置文件、依赖注入等全局性问题。
     */
    @Test
    void contextLoads() {
    }
}
