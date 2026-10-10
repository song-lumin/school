f = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\FoundItemServiceImpl.java'
c = open(f, encoding='utf-8').read()
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
if old in c:
    c = c.replace(old, new)
    open(f, 'w', encoding='utf-8').write(c)
    print('OK')
else:
    print('NOT FOUND')
