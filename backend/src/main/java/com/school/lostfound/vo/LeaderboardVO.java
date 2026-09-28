package com.school.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardVO {
    private List<Entry> entries;
    private Integer myRank;
    private Integer myScore;

    @Data
    @AllArgsConstructor
    public static class Entry {
        private Long userId;
        private String realName;
        private Integer creditScore;
    }
}
