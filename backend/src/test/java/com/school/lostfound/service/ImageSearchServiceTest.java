package com.school.lostfound.service;

import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.impl.ImageSearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageSearchServiceTest {
    @Mock private FoundItemMapper foundItemMapper;
    @Mock private ImageFingerprintService fingerprintService;
    @Mock private UserMapper userMapper;
    @Mock private DropPointMapper dropPointMapper;
    @InjectMocks private ImageSearchServiceImpl service;

    @Test
    void ranksNearestPublicImageFirstAndFiltersCategory() throws Exception {
        FoundItem near = item(1L, "电子产品", ItemStatus.PUBLIC.getCode());
        FoundItem far = item(2L, "电子产品", ItemStatus.PUBLIC.getCode());
        FoundItem otherCategory = item(3L, "钥匙", ItemStatus.PUBLIC.getCode());
        FoundItem nonPublic = item(4L, "电子产品", ItemStatus.PUBLISHED_NOT_HANDED_IN.getCode());
        when(foundItemMapper.selectList(any())).thenReturn(List.of(near, far, otherCategory, nonPublic));
        when(fingerprintService.hash(any())).thenReturn("0000000000000000");
        when(fingerprintService.getFingerprints(near)).thenReturn(List.of(
                fingerprint(near.getId(), "0000000000000003"),
                fingerprint(near.getId(), "0000000000000003")));
        when(fingerprintService.getFingerprints(far)).thenReturn(List.of(fingerprint(far.getId(), "000000000000ffff")));
        var results = service.searchByImage(image(), "电子产品");

        assertEquals(2, results.size());
        assertEquals(near.getId(), results.get(0).getItem().getId());
        assertEquals(97, results.get(0).getSimilarity());
        assertEquals(far.getId(), results.get(1).getItem().getId());
    }

    @Test
    void rejectsImagesThatCannotBeDecoded() throws Exception {
        doThrow(new IOException("bad image")).when(fingerprintService).hash(any());

        assertThrows(BusinessException.class, () -> service.searchByImage(image(), null));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "test.png", "image/png", new byte[]{1, 2, 3});
    }

    private FoundItem item(long id, String category, int status) {
        FoundItem item = new FoundItem();
        item.setId(id);
        item.setTitle("item " + id);
        item.setCategory(category);
        item.setItemStatus(status);
        item.setImages(List.of("/uploads/" + id + ".png"));
        return item;
    }

    private com.school.lostfound.entity.ItemImageFingerprint fingerprint(long itemId, String hash) {
        com.school.lostfound.entity.ItemImageFingerprint value = new com.school.lostfound.entity.ItemImageFingerprint();
        value.setItemId(itemId);
        value.setHashValue(hash);
        return value;
    }
}
