package com.xuetupt.controller;

import com.xuetupt.dto.Result;
import com.xuetupt.service.IStudyRoomService;
import com.xuetupt.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 自习室控制器
 * <p>
 * 提供自习室列表、进出房间、专注计时、排行榜、签到等功能接口。
 */
@RestController
@RequestMapping("/api/room")
public class StudyRoomController {

    @Resource
    private IStudyRoomService studyRoomService;

    @Resource
    private TokenUtils tokenUtils;

    /**
     * 获取自习室列表（含在线人数）
     */
    @GetMapping("/list")
    public Result list() {
        return studyRoomService.getRoomList();
    }

    /**
     * 进入自习室
     *
     * @param roomId 自习室ID
     * @param token  用户认证令牌
     */
    @PostMapping("/enter/{roomId}")
    public Result enter(@PathVariable("roomId") Long roomId, @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return studyRoomService.enterRoom(roomId, userId);
    }

    /**
     * 离开自习室
     *
     * @param roomId 自习室ID
     * @param token  用户认证令牌
     */
    @PostMapping("/leave/{roomId}")
    public Result leave(@PathVariable("roomId") Long roomId, @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return studyRoomService.leaveRoom(roomId, userId);
    }

    /**
     * 开始专注计时
     *
     * @param roomId 自习室ID
     * @param token  用户认证令牌
     */
    @PostMapping("/focus/start/{roomId}")
    public Result startFocus(@PathVariable("roomId") Long roomId, @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return studyRoomService.startFocus(roomId, userId);
    }

    /**
     * 结束专注计时
     *
     * @param roomId 自习室ID
     * @param token  用户认证令牌
     */
    @PostMapping("/focus/end/{roomId}")
    public Result endFocus(@PathVariable("roomId") Long roomId, @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return studyRoomService.endFocus(roomId, userId);
    }

    /**
     * 获取日排行榜
     */
    @GetMapping("/rank/daily")
    public Result dailyRank() {
        return studyRoomService.getDailyRank();
    }

    /**
     * 获取周排行榜
     */
    @GetMapping("/rank/weekly")
    public Result weeklyRank() {
        return studyRoomService.getWeeklyRank();
    }

    /**
     * 获取总排行榜
     */
    @GetMapping("/rank/total")
    public Result totalRank() {
        return studyRoomService.getTotalRank();
    }

    /**
     * 签到
     *
     * @param token 用户认证令牌
     */
    @PostMapping("/sign")
    public Result sign(@RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return studyRoomService.signIn(userId);
    }

    /**
     * 查询签到状态
     *
     * @param token 用户认证令牌
     * @return 签到状态（含连续签到天数）
     */
    @GetMapping("/sign/status")
    public Result signStatus(@RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return studyRoomService.getSignStatus(userId);
    }
}
