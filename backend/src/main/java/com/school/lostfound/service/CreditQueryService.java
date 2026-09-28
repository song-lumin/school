package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.vo.CertificateVO;
import com.school.lostfound.vo.LeaderboardVO;

public interface CreditQueryService {
    Integer getMyCredit(Long userId);

    IPage<CreditLog> listMyLogs(Long userId, int page, int size);

    LeaderboardVO getLeaderboard(Long currentUserId, int top);

    void updateLeaderboardSetting(Long userId, Integer allowLeaderboard);

    CertificateVO getCertificate(Long userId);
}
