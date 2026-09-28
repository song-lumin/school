package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.ItemImageFingerprint;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.ImageFingerprintService;
import com.school.lostfound.vo.FoundItemVO;
import com.school.lostfound.vo.ImageSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearchServiceImpl {
    private final FoundItemMapper foundItemMapper;
    private final ImageFingerprintService fingerprintService;
    private final UserMapper userMapper;
    private final DropPointMapper dropPointMapper;

    public List<ImageSearchResultVO> searchByImage(MultipartFile image, String category) {
        if (image == null || image.isEmpty() || image.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(400, "请上传不超过10MB的图片");
        }
        final String queryHash;
        try {
            queryHash = fingerprintService.hash(image);
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(400, "图片无法识别，请上传有效的 JPG、PNG 或 GIF 图片");
        }

        LambdaQueryWrapper<FoundItem> query = new LambdaQueryWrapper<>();
        query.eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode());
        if (StringUtils.hasText(category)) query.eq(FoundItem::getCategory, category);
        query.orderByDesc(FoundItem::getPublishedAt).last("LIMIT 200");
        List<FoundItem> candidates = foundItemMapper.selectList(query);

        return candidates.stream()
                .filter(item -> item.getItemStatus() != null && item.getItemStatus().equals(ItemStatus.PUBLIC.getCode()))
                .filter(item -> !StringUtils.hasText(category) || category.equals(item.getCategory()))
                .map(item -> bestMatch(item, queryHash))
                .filter(result -> result != null && result.getSimilarity() >= 30)
                .sorted(Comparator.comparingInt(ImageSearchResultVO::getSimilarity).reversed())
                .limit(10)
                .toList();
    }

    private ImageSearchResultVO bestMatch(FoundItem item, String queryHash) {
        List<ItemImageFingerprint> fingerprints = fingerprintService.getFingerprints(item);
        fingerprints = fingerprints.stream()
                .filter(fingerprint -> item.getId().equals(fingerprint.getItemId()))
                .toList();
        if (fingerprints.isEmpty()) return null;
        int distance = fingerprints.stream()
                .map(ItemImageFingerprint::getHashValue)
                .mapToInt(hash -> DifferenceHash.distance(queryHash, hash))
                .min().orElse(64);
        int similarity = Math.max(0, Math.round((64 - distance) * 100f / 64));
        return new ImageSearchResultVO(toVO(item), similarity);
    }

    private FoundItemVO toVO(FoundItem item) {
        var founder = item.getFounderId() == null ? null : userMapper.selectById(item.getFounderId());
        var point = item.getDropPointId() == null ? null : dropPointMapper.selectById(item.getDropPointId());
        String founderName = founder == null ? null : founder.getRealName();
        String pointName = point == null ? null : point.getName();
        return FoundItemVO.fromEntity(item, founderName, pointName, false);
    }
}
