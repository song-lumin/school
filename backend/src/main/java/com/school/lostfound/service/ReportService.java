package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.ReportCreateRequest;
import com.school.lostfound.dto.ReportHandleRequest;
import com.school.lostfound.vo.ReportVO;

public interface ReportService {

    ReportVO create(ReportCreateRequest request, Long reporterId);

    IPage<ReportVO> list(Integer status, String reportType, int page, int size);

    void handle(Long id, ReportHandleRequest request, Long operatorId);
}
