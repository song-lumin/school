package com.school.lostfound.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口 —— 统一处理图片文件的存储。
 * <p>
 * 业务背景：失物招领的核心信息载体是物品照片（拾获人上传拾获照片、
 * 失主上传寻物照片、用户上传头像等）。为避免文件直接塞进数据库导致
 * 膨胀，图片统一存放到服务器磁盘（或对象存储），数据库里只存可访问的 URL。
 * <p>
 * 上传时需要做安全校验：限制文件类型（只允许图片格式）、限制大小、
 * 用随机文件名替代原始文件名（防止路径穿越和重名覆盖）。
 * Controller 层只依赖本接口，存储方式的细节（本地磁盘/OSS）被封装在实现里，
 * 日后更换存储方案无需改动调用方。
 */
public interface FileService {
    /**
     * 上传一张图片并返回可访问的 URL。
     *
     * @param file 前端通过 multipart/form-data 上传的原始文件
     * @return 图片的访问地址（写入物品/头像等业务表中的 imageUrl 字段）
     *         校验失败（类型不符、超大、内容为空）时抛业务异常
     */
    String uploadImage(MultipartFile file);
}
