"""
一次性修复脚本：修改 FoundItemServiceImpl.java 的 forwardToNotice 方法。

用途：原实现只允许"发布者(founderId)"转发招领到寻物启事，
但业务上实际拾取者也应可以转发；同时原代码没有记录转发人。
此脚本把权限校验改为"校验物品状态"，并新增 forwarderId 字段
记录转发人，去掉 founderId 校验。

实现方式：脚本内以三引号字符串保存方法的旧代码(new 前)和新代码，
对源文件做全文查找替换；只有旧代码完整匹配才写回，否则打印
NOT FOUND 表示未找到目标代码（防止误改）。

输入：旧仓库路径下的 FoundItemServiceImpl.java（指向
     D:\codex\codex-data\schoollose 旧目录，历史遗留脚本）
输出：匹配成功则覆盖写回并打印 OK，否则不写文件打印 NOT FOUND
"""

# 目标 Java 服务实现类的绝对路径（旧项目目录下的路径）
f = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\FoundItemServiceImpl.java'
# 以 UTF-8 读入整个文件内容
c = open(f, encoding='utf-8').read()
# 旧版本方法：校验只有发布者可转发，且未记录转发人
old = '''    public void forwardToNotice(Long id, Long noticeId, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);
        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以转发招领");
        }
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())
                && !item.getItemStatus().equals(ItemStatus.CLAIMING.getCode())) {
            throw new BusinessException(409, "只有公开中的招领可以转发");
        }
        if (lostNoticeMapper.selectById(noticeId) == null) {
            throw new BusinessException(404, "寻物启事不存在");
        }
        item.setForwardedNoticeId(noticeId);
        foundItemMapper.updateById(item);
    }'''
# 新版本方法：去掉发布者校验（状态校验已足够），并记录转发人 forwarderId
new = '''    public void forwardToNotice(Long id, Long noticeId, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())
                && !item.getItemStatus().equals(ItemStatus.CLAIMING.getCode())) {
            throw new BusinessException(409, "只有公开中的招领可以转发");
        }
        if (lostNoticeMapper.selectById(noticeId) == null) {
            throw new BusinessException(404, "寻物启事不存在");
        }
        item.setForwardedNoticeId(noticeId);
        item.setForwarderId(currentUserId);
        foundItemMapper.updateById(item);
    }'''
# 上面 new 字符串与 old 的差异仅两处：删除了 founderId 的 403 校验、
# 新增 setForwarderId(currentUserId) 记录转发人（Java 代码，故字符串内不能加 # 注释）
if old in c:
    # 旧代码完整命中，替换后覆盖写回原文件
    c = c.replace(old, new)
    open(f, 'w', encoding='utf-8').write(c)
    print('OK')
else:
    # 未命中目标代码，不做任何修改
    print('NOT FOUND')

