package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.vo.DisputeVO;

/**
 * 争议（申诉）服务接口 —— 认领闭环后的纠纷处理通道。
 * <p>
 * 业务背景：物品认领完成后，可能出现"失主称未拿到物品""拾获人被
 * 恶意投诉""认领了但物品有损坏"等纠纷。线下处理缺乏留痕，所以系统
 * 内置争议流程：任何当事人在一定期限内发起争议 → 管理员在争议列表中
 * 逐条核实 → 出具处理结果（如撤销认领、回滚信用分、线下调解），全程记录。
 * <p>
 * 争议类型一般包括：物品未收到、物品状况与描述不符、冒领申诉等；
 * 状态流转为：待处理 → 已处理（含处理意见）。
 */
public interface DisputeService {
    /**
     * 当事人发起一条争议/申诉。
     *
     * @param request       争议内容（关联的认领单/物品、争议类型、描述、证据图片等）
     * @param currentUserId 当前登录用户 ID（校验其是否为该单据的当事人，防随意申诉他人单据）
     * @return 创建成功的争议详情 VO
     */
    DisputeVO createDispute(DisputeCreateRequest request, Long currentUserId);

    /**
     * 管理员分页筛选争议列表。
     *
     * @param disputeType 争议类型筛选，null 表示不过滤
     * @param status      处理状态筛选（待处理/已处理），null 表示不过滤
     * @param page        页码（从 1 开始）
     * @param size        每页条数
     * @return 争议分页列表（VO 含关联物品/认领单的展示信息）
     */
    IPage<DisputeVO> list(String disputeType, Integer status, int page, int size);

    /**
     * 查看争议详情（含权限校验：管理员可看全部，当事人只能看自己的）。
     *
     * @param id            争议 ID
     * @param currentUserId 当前用户 ID，用于越权校验
     * @return 争议详情 VO
     */
    DisputeVO getById(Long id, Long currentUserId);

    /**
     * 分页查询"与我相关"的争议（我发起的 + 涉及我的认领单的）。
     *
     * @param currentUserId 当前用户 ID
     * @param page          页码
     * @param size          每页条数
     * @return 争议分页列表
     */
    IPage<DisputeVO> listMy(Long currentUserId, int page, int size);

    /**
     * 管理员处理争议：填写处理意见与结论（如维持/撤销认领、是否回滚信用分）。
     * 处理后争议状态置为已处理；若结论涉及信用分变动，会联动调用 CreditService。
     *
     * @param id         争议 ID
     * @param request    处理结果（结论、意见）
     * @param operatorId 处理人（管理员）ID，写入记录便于审计
     */
    void handle(Long id, DisputeHandleRequest request, Long operatorId);
}
