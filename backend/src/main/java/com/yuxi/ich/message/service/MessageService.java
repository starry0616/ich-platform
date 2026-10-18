package com.yuxi.ich.message.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.message.entity.SysMessage;
import com.yuxi.ich.message.mapper.SysMessageMapper;
import com.yuxi.ich.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息通知服务（说明书表 6-4 接口 42）。
 *
 * <p>消息来源：报名成功、审核结果、互动回复、系统公告。
 * 写入统一走 {@link #send}，查询按当前登录用户隔离。
 */
@Service
public class MessageService {

    /** 消息类型：1 系统，2 报名，3 审核，4 互动 */
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_SIGNUP = 2;
    public static final int TYPE_AUDIT = 3;
    public static final int TYPE_INTERACTION = 4;

    private final SysMessageMapper messageMapper;

    public MessageService(SysMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /** 发送站内消息（业务模块调用，无需登录态） */
    @Transactional(rollbackFor = Exception.class)
    public void send(Long userId, int msgType, String title, String content, Long bizId) {
        if (userId == null) {
            return;
        }
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setMsgType(msgType);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setBizId(bizId);
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }

    /** 当前用户消息分页（新消息在前） */
    public PageResult<SysMessage> page(long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long offset = (pageNum - 1) * pageSize;
        List<SysMessage> records = messageMapper.selectList(
                Wrappers.<SysMessage>lambdaQuery()
                        .eq(SysMessage::getUserId, userId)
                        .orderByDesc(SysMessage::getIsRead)
                        .orderByDesc(SysMessage::getCreateTime)
                        .last("LIMIT " + offset + ", " + pageSize));
        long total = messageMapper.selectCount(
                Wrappers.<SysMessage>lambdaQuery().eq(SysMessage::getUserId, userId));
        return new PageResult<>(total, records, pageNum, pageSize);
    }

    /** 未读消息数 */
    public long unreadCount() {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return 0L;
        }
        Long count = messageMapper.selectCount(
                Wrappers.<SysMessage>lambdaQuery()
                        .eq(SysMessage::getUserId, userId)
                        .eq(SysMessage::getIsRead, 0));
        return count == null ? 0L : count;
    }

    /** 标记单条已读（只能读自己的） */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        Long userId = SecurityUtils.getUserId();
        SysMessage exist = messageMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作他人消息");
        }
        SysMessage update = new SysMessage();
        update.setId(id);
        update.setIsRead(1);
        messageMapper.updateById(update);
    }

    /** 全部标为已读 */
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long userId = SecurityUtils.getUserId();
        messageMapper.update(null, Wrappers.<SysMessage>lambdaUpdate()
                .set(SysMessage::getIsRead, 1)
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0));
    }
}
