package com.school.lostfound.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Spring MVC 配置类
 *
 * <p>解决「用户上传的图片如何通过 URL 访问」的问题。系统失物/招领的物品照片
 * 保存在服务器本地磁盘（由 application.yml 中 file.upload.path 指定的目录），
 * 而不是数据库或对象存储。本配置把 HTTP 路径 /uploads/** 映射到该磁盘目录，
 * 使前端可以通过 http://host:port/uploads/xxx.jpg 直接展示图片，
 * 同时配合 SecurityConfig 中对 /uploads/** 的放行，图片无需登录即可查看。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 从 application.yml 注入的上传文件存储目录（如 ./uploads） */
    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 注册静态资源映射：URL 路径 /uploads/** → 磁盘上的上传目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把配置的相对路径转为绝对路径，避免工作目录不同导致找不到文件
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().toString();
        // 访问 /uploads/** 时，到磁盘目录 file:绝对路径/ 下查找对应文件返回
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
