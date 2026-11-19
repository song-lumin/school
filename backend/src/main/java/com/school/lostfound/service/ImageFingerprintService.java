package com.school.lostfound.service;

import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.ItemImageFingerprint;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * 图像指纹服务接口 —— "以图搜图"能力的基础设施层。
 * <p>
 * 业务背景：失主常常说不清物品的具体文字特征，或同一物品在不同人
 * 口中描述差异很大。系统引入图像感知哈希（perceptual hash，如 pHash 类算法）：
 * 把每张物品图片压缩、灰度化后计算出一个紧凑的"指纹"字符串，
 * 两张图片指纹的汉明距离越小 → 视觉上越相似。这样失主上传一张照片，
 * 就能在海量招领图片中找出最相似的几件，大幅提升找回率。
 * <p>
 * 本接口负责指纹的生成、入库索引与读取；"按指纹搜索相似物品"的
 * 组合逻辑在 ImageSearchService 中完成。
 */
public interface ImageFingerprintService {
    /**
     * 计算一张图片的感知哈希指纹。
     *
     * @param file 上传的图片文件
     * @return 指纹字符串（如 64 位的 0/1 串或其十六进制形式）
     * @throws IOException 图片读取/解码失败
     */
    String hash(MultipartFile file) throws IOException;

    /**
     * 为一件招领物品的全部图片建立指纹索引（上传/编辑物品后调用）。
     * 逐张计算指纹并写入 item_image_fingerprint 表，供后续相似搜索匹配。
     *
     * @param item 已保存的招领物品（含图片 URL 列表）
     */
    void indexItemImages(FoundItem item);

    /**
     * 查询某物品已建立的图片指纹列表。
     *
     * @param item 招领物品
     * @return 该物品每张图片对应的指纹记录
     */
    List<ItemImageFingerprint> getFingerprints(FoundItem item);

    /**
     * 根据图片 URL 读取本地磁盘上的图片并解码为 BufferedImage。
     * <p>
     * 之所以要本地读取：指纹计算需要像素数据，而数据库中只存 URL；
     * 该方法负责把 URL 映射回磁盘路径并读入内存，供哈希/比对使用。
     *
     * @param imageUrl 图片访问地址
     * @return 解码后的图像对象
     * @throws IOException 文件不存在或解码失败
     */
    BufferedImage readLocalImage(String imageUrl) throws IOException;
}
