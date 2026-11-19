package com.school.lostfound.service;

import com.school.lostfound.dto.CheckRequest;
import com.school.lostfound.dto.DropPointRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.DropPointVO;
import com.school.lostfound.vo.InventoryItemVO;

import java.util.List;

/**
 * 失物招领点服务接口 —— 线下"投放点/保管点"的管理与台账能力。
 * <p>
 * 业务背景：拾获人不会永远替失主保管物品，系统设计了"投放点"机制：
 * 拾获人把物品投放到校内固定保管点（如宿管站、保卫处服务台），
 * 由投放点管理员签收入库、代为保管，认领人再到点内领取。
 * 这就产生三类线下业务，系统为其提供线上台账：
 * 1. 投放点信息维护（增删改查，前台展示给用户看"哪里可以投/领"）；
 * 2. 入库签收：拾获人点击"已投放"后，点内管理员核对并签收；
 * 3. 库存盘点：查看点内现存物品、待签收物品、待领取的认领单。
 */
public interface DropPointService {
    /**
     * 查询全部投放点（前台下拉选择、地图/列表展示共用）。
     *
     * @return 投放点列表（含名称、位置、开放时间、当前库存数等）
     */
    List<DropPointVO> listAll();

    /**
     * 查询单个投放点详情。
     *
     * @param id 投放点 ID
     * @return 投放点详情 VO；不存在时抛业务异常
     */
    DropPointVO getById(Long id);

    /**
     * 新建投放点（管理员操作）。
     *
     * @param request 投放点信息（名称、位置、负责人等）
     * @return 创建成功的投放点 VO
     */
    DropPointVO create(DropPointRequest request);

    /**
     * 修改投放点信息。
     *
     * @param id      投放点 ID
     * @param request 新的字段值
     * @return 更新后的投放点 VO
     */
    DropPointVO update(Long id, DropPointRequest request);

    /**
     * 删除投放点。若点内仍有在库物品，实现类应拒绝删除或要求先转移，
     * 防止出现"物品挂在已不存在的投放点"的悬空数据。
     *
     * @param id 投放点 ID
     */
    void delete(Long id);

    /**
     * 查询某投放点"待签收"的物品：拾获人已点击"已投放"但点内管理员
     * 尚未核对签收的物品，供投放点工作台展示。
     *
     * @param dropPointId 投放点 ID
     * @return 待签收物品列表
     */
    List<FoundItem> getPendingItems(Long dropPointId);

    /**
     * 查询某投放点"待领取"的认领单：审核已通过、等认领人到点取件的记录，
     * 供点内管理员核对身份后放行物品。
     *
     * @param dropPointId 投放点 ID
     * @return 待领取认领单列表
     */
    List<ClaimApplyVO> getPendingPickups(Long dropPointId);

    /**
     * 投放点签收/核对物品：管理员核对实物与系统记录一致后确认签收，
     * 物品状态随之流转（如"已入库"）；签收人（operatorId）入账留痕。
     *
     * @param dropPointId 投放点 ID（校验物品确实投放到该点）
     * @param request     签收信息（物品 ID、核对结果、备注等）
     * @param operatorId  操作的投放点管理员 ID，写入台账
     */
    void checkItem(Long dropPointId, CheckRequest request, Long operatorId);

    /**
     * 查询某投放点的在库物品清单（盘点用）。
     *
     * @param dropPointId 投放点 ID
     * @return 库存明细列表（含入库时间、状态、关联认领单情况）
     */
    List<InventoryItemVO> getInventory(Long dropPointId);
}
