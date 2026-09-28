package com.school.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageSearchResultVO {
    private FoundItemVO item;
    private int similarity;
}
