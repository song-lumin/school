package com.school.lostfound.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 *
 * <p>解决「每张表的创建时间/更新时间都要在业务代码里手动 set」的问题。
 * 实体类上标注了 @TableField(fill = FieldFill.INSERT) 的 createdAt 字段、
 * 以及 INSERT/UPDATE 时填充的 updatedAt 字段，会在执行 insert/update 时
 * 被本处理器自动填入当前时间，业务代码无需关心，保证时间字段统一、不遗漏。</p>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时填充：createdAt 与 updatedAt 均设为当前时间
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // strictInsertFill：仅当实体中该字段为 null 时才填充（不会覆盖调用方已显式设置的值）
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时填充：只刷新 updatedAt，createdAt 保持不变
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
