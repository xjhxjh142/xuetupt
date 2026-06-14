package com.xuetupt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.StudyRoom;

/**
 * 自习室服务接口
 * <p>
 * 提供自习室管理、专注计时、排行榜、签到等功能。
 */
public interface IStudyRoomService extends IService<StudyRoom> {
    /** 获取自习室列表（含在线人数） */
    Result getRoomList();

    /** 进入自习室 */
    Result enterRoom(Long roomId, Long userId);

    /** 离开自习室 */
    Result leaveRoom(Long roomId, Long userId);

    /** 开始专注计时 */
    Result startFocus(Long roomId, Long userId);

    /** 结束专注计时 */
    Result endFocus(Long roomId, Long userId);

    /** 获取日排行榜 */
    Result getDailyRank();

    /** 获取周排行榜 */
    Result getWeeklyRank();

    /** 获取总排行榜 */
    Result getTotalRank();

    /** 签到 */
    Result signIn(Long userId);

    /** 查询签到状态（含连续签到天数） */
    Result getSignStatus(Long userId);
}
