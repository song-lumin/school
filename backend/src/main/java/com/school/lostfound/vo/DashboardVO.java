package com.school.lostfound.vo;

import lombok.Data;

import java.util.Map;

@Data
public class DashboardVO {
    private Long totalItems;
    private Long publicItems;
    private Long pickedUpItems;
    private Long totalUsers;
    private Long totalClaims;
    private Long successClaims;
    private Map<String, Long> itemsByCategory;
    private Map<String, Long> claimsByDay;
}
