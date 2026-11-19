package com.school.lostfound.service.impl;

import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传服务实现类。
 * <p>
 * 功能：负责处理用户上传的图片文件（如失物招领的物品照片），
 * 将其保存到服务器本地磁盘，并返回可供前端直接访问的 URL。
 * <p>
 * 作用：失物招领业务强依赖图片——发布招领需上传物品照片，
 * 认领凭证、寻物启事也需要图片佐证。本类是所有图片上传的统一入口。
 * <p>
 * 为什么需要它：把"文件如何存储、如何校验、URL 如何生成"集中到一个地方，
 * 上层 Controller 只需调用一个方法即可，避免每个接口重复写保存逻辑；
 * 同时在这里统一做格式、大小校验，防止用户上传非法或超大文件占满磁盘。
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /** 上传文件在本地的存储根目录（从 application.yml 的 file.upload.path 读取） */
    @Value("${file.upload.path}")
    private String uploadPath;

    /** 生成图片 URL 时的前缀（如 /uploads），前端用该前缀 + 文件名拼接出可访问地址 */
    @Value("${file.upload.url-prefix}")
    private String urlPrefix;

    /**
     * 上传一张图片并返回其可访问的 URL。
     * <p>
     * 业务规则：
     * 1. 文件不能为空；
     * 2. 只允许 jpg / jpeg / png / gif 四种图片格式（按扩展名校验）；
     * 3. 大小不能超过 10MB；
     * 4. 保存时用 UUID 重命名文件，避免用户上传同名文件互相覆盖，
     *    同时避免中文/特殊字符文件名导致的路径问题。
     */
    @Override
    public String uploadImage(MultipartFile file) {
        // 空文件直接拒绝，防止产生 0 字节的垃圾文件
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        // 取原始文件名，用于识别扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(400, "文件名不能为空");
        }

        // 按扩展名白名单校验是否为图片，防止上传脚本、exe 等危险文件
        String extension = getFileExtension(originalFilename);
        if (!isImageFile(extension)) {
            throw new BusinessException(400, "只支持图片格式：jpg、jpeg、png、gif");
        }

        // 限制单文件 10MB，防止超大文件拖慢服务器或占满磁盘
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }

        try {
            // 上传目录不存在时自动创建（首次部署或目录被清理后仍能正常上传）
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 用随机 UUID 作为新文件名，保证唯一性；扩展名保持原样
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadDir.resolve(filename);
            // 将上传的文件内容写入本地磁盘
            file.transferTo(filePath.toFile());

            log.info("文件上传成功: {}", filename);
            // 返回带 URL 前缀的访问地址，前端直接用它展示图片
            return urlPrefix + "/" + filename;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }

    /**
     * 从文件名中提取扩展名（含点号，如 ".jpg"）；无点号时返回空串。
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * 判断扩展名是否属于允许上传的图片格式（白名单校验，忽略大小写）。
     */
    private boolean isImageFile(String extension) {
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") ||
               ext.equals(".png") || ext.equals(".gif");
    }
}
