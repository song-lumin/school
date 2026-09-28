package com.school.lostfound.dto;

import lombok.Data;

@Data
public class ItemQueryRequest {
    private String keyword;
    private String category;
    private Long dropPointId;
    private Integer itemStatus;
    private Integer page = 1;
    private Integer size = 10;
}
