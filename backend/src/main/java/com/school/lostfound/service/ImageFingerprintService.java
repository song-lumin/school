package com.school.lostfound.service;

import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.ItemImageFingerprint;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public interface ImageFingerprintService {
    String hash(MultipartFile file) throws IOException;

    void indexItemImages(FoundItem item);

    List<ItemImageFingerprint> getFingerprints(FoundItem item);

    BufferedImage readLocalImage(String imageUrl) throws IOException;
}
