package com.school.lostfound.service;

import com.school.lostfound.vo.RiskWarningVO;

import java.util.List;

public interface RiskControlService {
    /** 发布/认领文本敏感词检查，命中返回true */
    boolean containsSensitiveWord(String text);

    /** 串通检测：同一发布人-认领人完成领取 >= 2 次 */
    List<RiskWarningVO> detectCollusion();

    /** 认领率异常：发布人历史发布中成功领取比例过低 */
    List<RiskWarningVO> detectAbnormalClaimRate();

    /** 汇总全部风控预警 */
    List<RiskWarningVO> getAllWarnings();
}
