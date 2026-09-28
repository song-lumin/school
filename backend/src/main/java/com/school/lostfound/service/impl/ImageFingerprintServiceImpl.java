package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.ItemImageFingerprint;
import com.school.lostfound.mapper.ItemImageFingerprintMapper;
import com.school.lostfound.service.ImageFingerprintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageFingerprintServiceImpl implements ImageFingerprintService {
    private static final String ALGORITHM_VERSION = "dhash-64-v1";

    private final ItemImageFingerprintMapper fingerprintMapper;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/uploads}")
    private String uploadUrlPrefix;

    @Override
    public String hash(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) throw new IOException("Unsupported image data");
        return DifferenceHash.hash(image);
    }

    @Override
    public void indexItemImages(FoundItem item) {
        if (item.getId() == null || item.getImages() == null) return;
        for (String imageUrl : item.getImages()) {
            Path localImage = resolveLocalImage(imageUrl);
            if (localImage == null) continue;
            try {
                LambdaQueryWrapper<ItemImageFingerprint> query = new LambdaQueryWrapper<>();
                query.eq(ItemImageFingerprint::getItemId, item.getId())
                        .eq(ItemImageFingerprint::getImageUrl, imageUrl);
                if (fingerprintMapper.selectCount(query) > 0) continue;
                BufferedImage image = ImageIO.read(localImage.toFile());
                if (image == null) continue;
                ItemImageFingerprint fingerprint = new ItemImageFingerprint();
                fingerprint.setItemId(item.getId());
                fingerprint.setImageUrl(imageUrl);
                fingerprint.setHashValue(DifferenceHash.hash(image));
                fingerprint.setAlgorithmVersion(ALGORITHM_VERSION);
                fingerprintMapper.insert(fingerprint);
            } catch (IOException | RuntimeException exception) {
                log.warn("无法生成物品图片指纹: itemId={}, url={}", item.getId(), imageUrl, exception);
            }
        }
    }

    @Override
    public List<ItemImageFingerprint> getFingerprints(FoundItem item) {
        indexItemImages(item);
        LambdaQueryWrapper<ItemImageFingerprint> query = new LambdaQueryWrapper<>();
        query.eq(ItemImageFingerprint::getItemId, item.getId());
        return fingerprintMapper.selectList(query);
    }

    @Override
    public BufferedImage readLocalImage(String imageUrl) throws IOException {
        Path path = resolveLocalImage(imageUrl);
        if (path == null) return null;
        return ImageIO.read(path.toFile());
    }

    private Path resolveLocalImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(uploadUrlPrefix + "/")) return null;
        String fileName = imageUrl.substring(uploadUrlPrefix.length() + 1);
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")) return null;
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path path = root.resolve(fileName).normalize();
        return path.startsWith(root) ? path : null;
    }

}
