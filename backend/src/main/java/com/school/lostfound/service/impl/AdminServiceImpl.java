package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.AdminService;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.vo.DashboardVO;
import com.school.lostfound.vo.ExpireWarningVO;
import com.school.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final FoundItemMapper foundItemMapper;
    private final ClaimApplyMapper claimApplyMapper;
    private final CreditLogMapper creditLogMapper;
    private final CreditService creditService;

    @Override
    public IPage<UserVO> listUsers(String keyword, String role, Integer status, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getStudentId, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, UserRole.valueOf(role));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreatedAt);

        IPage<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, String role) {
        User user = getUserOrThrow(userId);

        UserRole newRole;
        try {
            newRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "无效的角色：" + role);
        }

        user.setRole(newRole);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = getUserOrThrow(userId);

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "无效的状态值");
        }

        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public DashboardVO getDashboard() {
        DashboardVO dashboard = new DashboardVO();

        dashboard.setTotalItems(foundItemMapper.selectCount(null));
        dashboard.setPublicItems(foundItemMapper.selectCount(
                new LambdaQueryWrapper<FoundItem>().eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode())));
        dashboard.setPickedUpItems(foundItemMapper.selectCount(
                new LambdaQueryWrapper<FoundItem>().eq(FoundItem::getItemStatus, ItemStatus.PICKED_UP.getCode())));

        dashboard.setTotalUsers(userMapper.selectCount(null));
        dashboard.setTotalClaims(claimApplyMapper.selectCount(null));
        dashboard.setSuccessClaims(claimApplyMapper.selectCount(
                new LambdaQueryWrapper<ClaimApply>().eq(ClaimApply::getApplyStatus, ClaimStatus.COMPLETED.getCode())));

        List<FoundItem> items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>().select(FoundItem::getCategory));
        Map<String, Long> itemsByCategory = items.stream()
                .collect(Collectors.groupingBy(FoundItem::getCategory, Collectors.counting()));
        dashboard.setItemsByCategory(itemsByCategory);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<ClaimApply> recentClaims = claimApplyMapper.selectList(
                new LambdaQueryWrapper<ClaimApply>()
                        .ge(ClaimApply::getCreatedAt, sevenDaysAgo)
                        .eq(ClaimApply::getApplyStatus, ClaimStatus.COMPLETED.getCode()));
        Map<String, Long> claimsByDay = recentClaims.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()));
        dashboard.setClaimsByDay(claimsByDay);

        return dashboard;
    }

    @Override
    public List<ExpireWarningVO> getExpireWarnings() {
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode())
                .eq(FoundItem::getExpireWarningSent, 1);

        List<FoundItem> items = foundItemMapper.selectList(wrapper);

        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ExpireWarningVO vo = new ExpireWarningVO();
                    vo.setItemId(item.getId());
                    vo.setTitle(item.getTitle());
                    vo.setPublishedAt(item.getPublishedAt());
                    vo.setPerishable(item.getPerishable());

                    User founder = userMapper.selectById(item.getFounderId());
                    vo.setFounderName(founder != null ? founder.getRealName() : null);

                    vo.setDaysSincePublish(
                            ChronoUnit.DAYS.between(item.getPublishedAt(), now));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void rollbackCredit(Map<String, Object> request, Long operatorId) {
        Object itemIdObj = request.get("itemId");
        Object applyIdObj = request.get("applyId");
        Object reasonObj = request.get("reason");

        if (itemIdObj == null) {
            throw new BusinessException(400, "物品ID不能为空");
        }
        if (reasonObj == null || reasonObj.toString().isBlank()) {
            throw new BusinessException(400, "回滚原因不能为空");
        }

        Long itemId = Long.valueOf(itemIdObj.toString());
        Long applyId = applyIdObj != null ? Long.valueOf(applyIdObj.toString()) : null;
        String reason = reasonObj.toString();

        FoundItem item = foundItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        rollbackUserCredit(publisherId, itemId, applyId, "PICKUP_ISSUE", 3, reason, operatorId);
        rollbackUserCredit(publisherId, itemId, applyId, "CHECK_ISSUE", 1, reason, operatorId);

        if (applyId != null) {
            ClaimApply apply = claimApplyMapper.selectById(applyId);
            if (apply != null) {
                rollbackUserCredit(apply.getClaimerId(), itemId, applyId, "PICKUP_ISSUE", 1, reason, operatorId);
                apply.setCreditRollback(1);
                claimApplyMapper.updateById(apply);
            }
        }

        item.setItemStatus(ItemStatus.VOIDED.getCode());
        foundItemMapper.updateById(item);
    }

    private void rollbackUserCredit(Long userId, Long itemId, Long applyId,
                                    String operationType, int amount,
                                    String reason, Long operatorId) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, operationType)
                .eq(CreditLog::getRelatedItemId, itemId);

        if (creditLogMapper.selectCount(wrapper) == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, -amount);

        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return;
        }

        creditService.issueCredit(userId, -amount, "ROLLBACK", itemId, applyId, reason, operatorId);
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public void adjustCredit(Long userId, Integer newScore, String reason, Long operatorId) {
        if (newScore == null || newScore < 0) {
            throw new BusinessException(400, "积分必须为非负整数");
        }
        User user = getUserOrThrow(userId);
        int oldScore = user.getCreditScore() != null ? user.getCreditScore() : 0;
        int diff = newScore - oldScore;
        if (diff == 0) return;
        user.setCreditScore(newScore);
        userMapper.updateById(user);
        creditService.issueCredit(userId, diff, "ADMIN_ADJUST", null, null,
                reason != null ? reason : "管理员调整积分", operatorId);
    }

    @Override
    public java.util.List<com.school.lostfound.entity.CreditLog> userCreditLogs(Long userId) {
        LambdaQueryWrapper<com.school.lostfound.entity.CreditLog> w = new LambdaQueryWrapper<>();
        w.eq(com.school.lostfound.entity.CreditLog::getUserId, userId)
         .orderByDesc(com.school.lostfound.entity.CreditLog::getCreatedAt);
        return creditLogMapper.selectList(w);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
