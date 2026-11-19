package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.HandInLog;
import com.school.lostfound.entity.ReportLog;
import com.school.lostfound.entity.Dispute;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.HandInLogMapper;
import com.school.lostfound.mapper.LostNoticeMapper;
import com.school.lostfound.mapper.ReportLogMapper;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.FoundItemService;
import com.school.lostfound.service.ImageFingerprintService;
import com.school.lostfound.service.RiskControlService;
import com.school.lostfound.vo.FoundItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 失物招领（FoundItem）业务逻辑实现类。
 * <p>
 * 功能：管理"捡到物品招领"的完整生命周期——发布、公开展示、
 * 确认投放、转发给寻物启事、作废、归档、管理员删除等。
 * <p>
 * 作用：本系统采用"两段式发布"流程：发布者先线上登记招领信息（状态为
 * 待投放），再把实物放入校园投放点后点击"已投放"，物品才对外公开可认领。
 * 本类承载了这套状态机的核心流转规则，是与数据库交互、协调风控、
 * 图片指纹等各服务的业务中枢。
 * <p>
 * 为什么需要它：Controller 层只做参数接收与校验，复杂的业务规则
 * （每周发布限额、敏感词过滤、状态流转合法性、权限归属判断）
 * 都集中在 Service 层处理，保证业务规则统一、可测试。
 */
@Service
@RequiredArgsConstructor
public class FoundItemServiceImpl implements FoundItemService {

    /** 每个用户每周最多发布 3 条"有效"招领（防刷量/垃圾信息） */
    private static final int MAX_WEEKLY_PUBLISH = 3;
    /** 注册后 7 天内发布的招领视为"新用户发布"，前端据此展示引导 */
    private static final int NEW_USER_DAYS = 7;

    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;
    private final UserMapper userMapper;
    private final DropPointMapper dropPointMapper;
    private final RiskControlService riskControlService;
    private final ImageFingerprintService imageFingerprintService;
    private final DisputeMapper disputeMapper;
    private final ReportLogMapper reportLogMapper;
    private final LostNoticeMapper lostNoticeMapper;

    /**
     * 发布一条失物招领。
     * <p>
     * 业务规则：
     * 1. 校验每周发布限额（只统计未结束的物品，已归档/作废不占额度）；
     * 2. 标题与描述经过敏感词过滤，命中则拒绝发布；
     * 3. 必须选择一个状态为启用中的投放点；
     * 4. 新发布的物品状态为"待投放"(6)，不对外公开——发布人线下把
     *    实物放入投放点后调用 markPlaced 才变为公开(1)；
     * 5. 支持代他人登记（actualFounderId），记录真正的拾取人；
     * 6. 同时插入 hand_in_log 实物交接记录，初始状态 0=实物未投放；
     * 7. 发布成功后异步补建图片指纹（供以图搜物用），失败仅告警不回滚。
     * <p>
     * 为什么用 @Transactional：本方法同时写 found_item 与 hand_in_log
     * 两张表，事务保证两者要么都成功、要么都回滚，避免出现"有物品
     * 但无交接记录"的脏数据。
     */
    @Override
    @Transactional
    public FoundItemVO publish(FoundItemRequest request, Long currentUserId) {
        // 发布前先检查该用户近 7 天内有效招领数量是否已达上限
        checkWeeklyLimit(currentUserId);

        // 标题或描述命中敏感词库则直接拒绝（风控，防止发布违规内容）
        if (riskControlService.containsSensitiveWord(request.getTitle())
                || riskControlService.containsSensitiveWord(request.getDescription())) {
            throw new BusinessException(400, "内容包含敏感词，请修改后重新发布");
        }

        // 校验发布人存在（JWT 中的用户 ID 必须能在库中找到）
        User founder = userMapper.selectById(currentUserId);
        if (founder == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 校验投放点存在且处于启用状态（status=1），停用的投放点不能收新物品
        DropPoint dropPoint = dropPointMapper.selectById(request.getDropPointId());
        if (dropPoint == null || dropPoint.getStatus() != 1) {
            throw new BusinessException(404, "投放点不存在或已停用");
        }

        FoundItem item = new FoundItem();
        item.setTitle(request.getTitle());
        item.setCategory(request.getCategory());
        item.setDescription(request.getDescription());
        item.setFoundLocation(request.getFoundLocation());
        item.setFoundTime(request.getFoundTime());
        item.setImages(request.getImages());
        item.setClaimQuestion(request.getClaimQuestion());
        item.setReferenceAnswer(request.getReferenceAnswer());
        // 易腐物品标志：前端未传时默认 0（非易腐），易腐物品巡检/过期策略不同
        item.setPerishable(request.getPerishable() != null ? request.getPerishable() : 0);
        // 发布后先为"已发布待投放"(6)，不对外公开；发布人线下把实物放入投放点后点"已投放"才变公开(1)
        item.setItemStatus(ItemStatus.PENDING_PLACEMENT.getCode());
        item.setFounderId(currentUserId);
        item.setDropPointId(request.getDropPointId());

        // 允许代他人登记：若指定了实际拾取人且不是发布人本人，则校验该用户存在
        if (request.getActualFounderId() != null && !request.getActualFounderId().equals(currentUserId)) {
            User actualFounder = userMapper.selectById(request.getActualFounderId());
            if (actualFounder == null) {
                throw new BusinessException(404, "实际拾取者不存在");
            }
            item.setActualFounderId(request.getActualFounderId());
        }

        // 记录发布时间，作为每周限额统计与"新用户"判断的时间基准
        item.setPublishedAt(LocalDateTime.now());
        foundItemMapper.insert(item);

        // hand_in_log 初始状态0=实物未投放（发布人尚未把实物放入投放点）
        HandInLog log = new HandInLog();
        log.setItemId(item.getId());
        log.setDropPointId(request.getDropPointId());
        log.setHandInStatus(0);
        handInLogMapper.insert(log);

        // 为物品图片建立 dhash 指纹索引，供后续"以图搜物"匹配；
        // 指纹建立失败不影响发布主流程，仅记录告警（可在查询时补建）
        try {
            imageFingerprintService.indexItemImages(item);
        } catch (RuntimeException exception) {
            org.slf4j.LoggerFactory.getLogger(FoundItemServiceImpl.class)
                    .warn("物品图片指纹建立失败: itemId={}", item.getId(), exception);
        }

        return FoundItemVO.fromEntity(item, founder.getRealName(), null,
                isNewUser(founder, item));
    }

    /**
     * 分页查询对外公开的招领列表（首页/列表页使用）。
     * <p>
     * 支持筛选：关键词（标题/描述模糊匹配）、分类、投放点；
     * 显式传 itemStatus 时按该状态查（如冒领申诉流程需要列出已取件的物品），
     * 否则默认只查公开状态(1)的物品。
     * 按发布时间倒序排列，最新发布的排在前面。
     */
    @Override
    public IPage<FoundItemVO> listPublic(ItemQueryRequest request) {
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        // 默认查公开物品；显式传 itemStatus 时按传入状态查（如冒领申诉需列出已取件物品）
        if (request.getItemStatus() != null) {
            wrapper.eq(FoundItem::getItemStatus, request.getItemStatus());
        } else {
            wrapper.eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode());
        }

        // 关键词对标题或描述做模糊匹配（or 拼接，任一命中即返回）
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(FoundItem::getTitle, request.getKeyword())
                    .or().like(FoundItem::getDescription, request.getKeyword()));
        }
        // 按物品分类精确筛选（如"证件"、"电子产品"）
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(FoundItem::getCategory, request.getCategory());
        }
        // 按投放点筛选，方便用户只看离家/教室近的投放点物品
        if (request.getDropPointId() != null) {
            wrapper.eq(FoundItem::getDropPointId, request.getDropPointId());
        }

        wrapper.orderByDesc(FoundItem::getPublishedAt);

        // MyBatis-Plus 物理分页查询（需分页插件支持）
        IPage<FoundItem> page = foundItemMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);

        return convertPage(page);
    }

    /**
     * 按 ID 查询单条招领详情，不存在则抛 404 业务异常。
     */
    @Override
    public FoundItemVO getById(Long id) {
        FoundItem item = getItemOrThrow(id);
        // 复用批量转换逻辑，为单条记录补齐发布人姓名、投放点名称等展示字段
        return convertWithNames(List.of(item)).get(0);
    }

    /**
     * 发布者作废自己发布的招领。
     * <p>
     * 业务规则：
     * 1. 只有发布者本人可以作废（403）；
     * 2. 仅允许在"待投放"(6)或"公开"(1)状态下作废——已被认领中/已取件
     *    的物品涉及他人权益，不能随意作废（409）；
     * 3. 作废同时把 hand_in_log 交接记录置为 3（作废），保持两表状态一致。
     * <p>
     * 用 @Transactional 保证物品状态与交接记录状态原子更新。
     */
    @Override
    @Transactional
    public void invalidate(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);

        // 权限校验：只有发布者本人能作废自己的招领
        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以作废");
        }
        // 允许在待投放(6)或公开(1)状态下作废
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())
                && !item.getItemStatus().equals(ItemStatus.PENDING_PLACEMENT.getCode())) {
            throw new BusinessException(409, "物品当前状态不可作废");
        }

        // 物品状态 → 作废(2)
        item.setItemStatus(ItemStatus.VOIDED.getCode());
        foundItemMapper.updateById(item);

        // 同步更新交接记录为 3（作废），避免遗留"悬空"的在途交接记录
        LambdaQueryWrapper<HandInLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandInLog::getItemId, id);
        HandInLog log = handInLogMapper.selectOne(wrapper);
        if (log != null) {
            log.setHandInStatus(3);
            handInLogMapper.updateById(log);
        }
    }

    /**
     * 发布者确认"实物已放入投放点"，物品由待投放转为公开可认领。
     * <p>
     * 这是两段式发布的第二段：只有待投放(6)状态的物品且由发布者本人
     * 操作才允许（否则 403/409）。物品状态 → 公开(1)；
     * hand_in_log 状态 0(未投放) → 1(已放置待巡检)，并记录实际投放时间。
     * <p>
     * 用 @Transactional 保证物品与交接记录两处状态同时变更。
     */
    @Override
    @Transactional
    public void markPlaced(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);

        // 只有发布者本人可以确认投放
        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以确认已投放");
        }
        // 状态机校验：只有待投放(6)状态才能进入公开(1)
        if (!item.getItemStatus().equals(ItemStatus.PENDING_PLACEMENT.getCode())) {
            throw new BusinessException(409, "只有待投放状态的物品可以标记已投放");
        }

        // 物品转为公开待认领
        item.setItemStatus(ItemStatus.PUBLIC.getCode());
        foundItemMapper.updateById(item);

        // hand_in_log: 0(未投放) → 1(已放置实物待巡检)
        LambdaQueryWrapper<HandInLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandInLog::getItemId, id);
        HandInLog log = handInLogMapper.selectOne(wrapper);
        if (log != null) {
            log.setHandInStatus(1);
            // 记录实物实际放入投放点的时间，供巡检/超期计算使用
            log.setHandedInAt(LocalDateTime.now());
            handInLogMapper.updateById(log);
        }
    }

    /**
     * 把一条招领转发给某条寻物启事（失主登记的寻物信息）。
     * <p>
     * 场景：用户浏览招领时发现某条正好匹配别人的寻物启事，
     * 可一键转发提醒双方对接。业务规则：
     * 1. 只有公开(1)或认领中(2)状态的招领可以转发；
     * 2. 目标寻物启事必须存在；
     * 3. 记录转发人与目标启事 ID，前端据此在启事页展示关联招领。
     */
    @Override
    @Transactional
    public void forwardToNotice(Long id, Long noticeId, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);
        // 已取件/已归档/已作废的历史物品没有转发价值，禁止转发
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())
                && !item.getItemStatus().equals(ItemStatus.CLAIMING.getCode())) {
            throw new BusinessException(409, "只有公开中的招领可以转发");
        }
        // 目标寻物启事必须真实存在
        if (lostNoticeMapper.selectById(noticeId) == null) {
            throw new BusinessException(404, "寻物启事不存在");
        }
        // 记录关联关系与转发人，便于追溯谁做的匹配
        item.setForwardedNoticeId(noticeId);
        item.setForwarderId(currentUserId);
        foundItemMapper.updateById(item);
    }

    /**
     * 归档招领，按原状态区分两类操作人：
     * 1. 已取件(3) → 归档(4)：发布者确认物品已被领走、流程收尾；
     * 2. 超期(5) → 归档(4)：超期物品经线下处置后，只有系统管理员可以归档，
     *    普通用户无权处理公共资产（403）。
     * 其余状态的物品不允许归档（409）。
     */
    @Override
    @Transactional
    public void archive(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);
        int status = item.getItemStatus();

        if (status == ItemStatus.PICKED_UP.getCode()) {
            // 3→4：发布者手动归档已取件物品
            if (!item.getFounderId().equals(currentUserId)) {
                throw new BusinessException(403, "只有发布者可以归档");
            }
        } else if (status == ItemStatus.EXPIRED.getCode()) {
            // 5→4：超期物品经线下处置后由管理员归档
            // 需要校验操作人角色为系统管理员，防止普通用户清理他人/公共物品
            User operator = userMapper.selectById(currentUserId);
            if (operator == null || operator.getRole() != com.school.lostfound.enums.UserRole.SYS_ADMIN) {
                throw new BusinessException(403, "只有系统管理员可以归档超期物品");
            }
        } else {
            // 状态机不允许从其他状态直接跳到归档
            throw new BusinessException(409, "物品当前状态不可归档");
        }

        item.setItemStatus(ItemStatus.ARCHIVED.getCode());
        foundItemMapper.updateById(item);
    }

    /**
     * 分页查询当前用户自己发布的所有招领（"我的发布"页面）。
     * 可选按状态过滤，按发布时间倒序。
     */
    @Override
    public IPage<FoundItemVO> listMy(Long currentUserId, ItemQueryRequest request) {
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        // 只查自己发布的物品（数据归属隔离）
        wrapper.eq(FoundItem::getFounderId, currentUserId);

        // 可选按状态过滤（如只看"公开中"或"已取件"）
        if (request.getItemStatus() != null) {
            wrapper.eq(FoundItem::getItemStatus, request.getItemStatus());
        }

        wrapper.orderByDesc(FoundItem::getPublishedAt);

        IPage<FoundItem> page = foundItemMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);

        return convertPage(page);
    }

    /**
     * 校验用户的每周发布限额。
     * <p>
     * 规则：统计该用户近 7 天内发布、且状态仍在"有效流转中"
     * （公开 1 / 认领中 2 / 已取件 3）的招领数量，达到 3 条则拒绝发布。
     * 已作废/已归档/超期的物品不占额度，避免正常使用被误伤。
     */
    private void checkWeeklyLimit(Long userId) {        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getFounderId, userId)
                .in(FoundItem::getItemStatus,
                        ItemStatus.PUBLIC.getCode(),
                        ItemStatus.CLAIMING.getCode(),
                        ItemStatus.PICKED_UP.getCode())
                // 只统计近 7 天（一周）内发布的记录
                .ge(FoundItem::getPublishedAt, weekAgo);
        if (foundItemMapper.selectCount(wrapper) >= MAX_WEEKLY_PUBLISH) {
            throw new BusinessException(429, "每周最多发布3条有效招领，请下周再试");
        }
    }

    /**
     * 把 FoundItem 实体分页结果转换为 VO 分页结果（补齐展示字段）。
     */
    private IPage<FoundItemVO> convertPage(IPage<FoundItem> page) {
        List<FoundItemVO> voList = convertWithNames(page.getRecords());
        // 保留原分页参数与总条数，只替换记录列表为 VO 类型
        Page<FoundItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 批量把 FoundItem 实体列表转换为 VO 列表。
     * <p>
     * 为什么用批量查询：如果对每条记录单独查发布人姓名、投放点名称，
     * 一页 20 条会产生几十次数据库查询（N+1 问题）。这里先收集所有
     * 去重后的 founderId / dropPointId，各做一次批量查询，再在内存中
     * 组装成 Map 供逐条映射，把查询次数降为常数级。
     */
    private List<FoundItemVO> convertWithNames(List<FoundItem> items) {
        // 收集所有非空的发布人 ID 并去重，用于批量查询用户
        List<Long> founderIds = items.stream()
                .map(FoundItem::getFounderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 收集所有非空的投放点 ID 并去重，用于批量查询投放点
        List<Long> dropPointIds = items.stream()
                .map(FoundItem::getDropPointId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 一次查出本页涉及的所有发布人用户记录
        List<User> founders = founderIds.isEmpty() ? List.of()
                : userMapper.selectBatchIds(founderIds);

        // id → 真实姓名 的映射，供 VO 展示用
        Map<Long, String> founderNames = founders.stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));

        // id → User 的映射（key 冲突时保留先出现的，避免重复 ID 抛异常），供新用户判断用
        Map<Long, User> foundersById = founders.stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a,b) -> a));

        // id → 投放点名称 的映射，一次批量查询
        Map<Long, String> dropPointNames = dropPointIds.isEmpty() ? java.util.Collections.emptyMap()
                : dropPointMapper.selectBatchIds(dropPointIds).stream()
                        .collect(Collectors.toMap(DropPoint::getId, DropPoint::getName));

        return items.stream()
                .map(item -> FoundItemVO.fromEntity(
                        item,
                        founderNames.get(item.getFounderId()),
                        dropPointNames.get(item.getDropPointId()),
                        isNewUser(foundersById.get(item.getFounderId()), item)))
                .collect(Collectors.toList());
    }

    /**
     * 判断该招领是否由"新注册用户"发布。
     * 规则：发布时间早于用户注册时间 + 7 天，即注册后一周内发布。
     * 前端据此给新用户的发布加引导/提示标签。
     */
    private boolean isNewUser(User founder, FoundItem item) {
        return founder != null && founder.getCreatedAt() != null && item.getPublishedAt() != null
                && item.getPublishedAt().isBefore(founder.getCreatedAt().plusDays(NEW_USER_DAYS));
    }

    /**
     * 管理员硬删除一条招领记录。
     * <p>
     * 注意：dispute（申诉）与 report_log（举报）表对 found_item 的外键
     * 没有配置级联删除，直接删物品会被外键约束阻塞，所以必须先手动
     * 删除这两张表中该物品的关联数据，再删除物品本身。
     * 用 @Transactional 保证三步删除的原子性。
     */
    @Override
    @Transactional
    public void deleteByAdmin(Long id) {
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }

        // dispute/report_log 外键无级联，需先删，否则删除被阻塞
        disputeMapper.delete(new LambdaQueryWrapper<Dispute>().eq(Dispute::getItemId, id));
        reportLogMapper.delete(new LambdaQueryWrapper<ReportLog>().eq(ReportLog::getItemId, id));
        foundItemMapper.deleteById(id);
    }

    /**
     * 按 ID 取物品，不存在则抛 404 业务异常（多处复用的兜底查询）。
     */
    private FoundItem getItemOrThrow(Long id) {
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        return item;
    }
}
