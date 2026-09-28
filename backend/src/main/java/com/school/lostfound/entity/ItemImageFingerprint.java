package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("item_image_fingerprint")
public class ItemImageFingerprint {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    private String imageUrl;

    private String hashValue;

    private String algorithmVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
