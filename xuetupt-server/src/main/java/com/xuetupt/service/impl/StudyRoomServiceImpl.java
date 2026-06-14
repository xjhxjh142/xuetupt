package com.xuetupt.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.FocusRecord;
import com.xuetupt.entity.StudyRoom;
import com.xuetupt.mapper.FocusRecordMapper;
import com.xuetupt.mapper.StudyRoomMapper;
import com.xuetupt.service.IStudyRoomService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.xuetupt.utils.RedisConstants.*;

/**
 * 自习室服务实现
 * <p>
 * 实现自习室管理、专注计时、排行榜、签到等功能。
 * 使用 Redis Set 管理在线用户，ZSet 记录专注时长排行榜，BitMap 记录签到数据。
 */
@Service
public class StudyRoomServiceImpl extends ServiceImpl<StudyRoomMapper, StudyRoom> implements IStudyRoomService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FocusRecordMapper focusRecordMapper;

    /**
     * 获取自习室列表（含在线人数）
     * <p>
     * 从 Redis Set 中获取每个自习室的当前在线人数（Set 的 size）。
     */
    @Override
    public Result getRoomList() {
        List<StudyRoom> rooms = list();
        for (StudyRoom room : rooms) {
            Long count = stringRedisTemplate.opsForSet().size(ROOM_ONLINE_KEY + room.getId());
            room.setCurrentCount(count != null ? count.intValue() : 0);
        }
        return Result.ok(rooms);
    }

    /**
     * 进入自习室
     * <p>
     * 将 userId 加入自习室的在线 Set，并设置 30 分钟过期，
     * 防止用户断开连接后在线人数只增不减。
     */
    @Override
    public Result enterRoom(Long roomId, Long userId) {
        String key = ROOM_ONLINE_KEY + roomId;
        stringRedisTemplate.opsForSet().add(key, userId.toString());
        // 设置过期时间，防止用户断开连接后人数只增不减
        stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES);
        return Result.ok();
    }

    /**
     * 离开自习室
     * <p>
     * 将 userId 从自习室的在线 Set 中移除。
     */
    @Override
    public Result leaveRoom(Long roomId, Long userId) {
        stringRedisTemplate.opsForSet().remove(ROOM_ONLINE_KEY + roomId, userId.toString());
        return Result.ok();
    }

    /**
     * 开始专注计时
     * <p>
     * 将开始时间记录到 Redis 中，同时将用户加入自习室在线 Set。
     */
    @Override
    public Result startFocus(Long roomId, Long userId) {
        String key = FOCUS_START_KEY + userId + ":" + roomId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
        // 开始专注时自动进入自习室
        enterRoom(roomId, userId);
        return Result.ok("开始专注");
    }

    /**
     * 结束专注计时
     * <p>
     * 计算专注时长（至少1分钟），保存到数据库，并更新 Redis 排行榜。
     *
     * @return 本次专注时长
     */
    @Override
    public Result endFocus(Long roomId, Long userId) {
        String key = FOCUS_START_KEY + userId + ":" + roomId;
        String startTimeStr = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(startTimeStr)) {
            return Result.fail("未开始专注");
        }

        long startMs = Long.parseLong(startTimeStr);
        long endMs = System.currentTimeMillis();
        // 至少算1分钟
        int durationMinutes = Math.max(1, (int) ((endMs - startMs) / 60000));

        // 保存到数据库
        FocusRecord record = new FocusRecord();
        record.setUserId(userId);
        record.setRoomId(roomId);
        record.setFocusDate(LocalDate.now());
        record.setDurationMinutes(durationMinutes);
        // 使用真实的开始时间（从时间戳转换）
        record.setStartTime(LocalDateTime.ofEpochSecond(startMs / 1000, 0, java.time.ZoneOffset.ofHours(8)));
        record.setEndTime(LocalDateTime.now());
        focusRecordMapper.insert(record);

        // 更新 ZSet 排行榜（日/周/总）
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        stringRedisTemplate.opsForZSet().incrementScore(RANK_FOCUS_DAILY_KEY + today, userId.toString(), durationMinutes);
        stringRedisTemplate.opsForZSet().incrementScore(RANK_FOCUS_WEEKLY_KEY + getWeekKey(), userId.toString(), durationMinutes);
        stringRedisTemplate.opsForZSet().incrementScore(RANK_FOCUS_TOTAL_KEY, userId.toString(), durationMinutes);

        // 删除开始标记
        stringRedisTemplate.delete(key);

        return Result.ok("本次专注 " + durationMinutes + " 分钟");
    }

    /**
     * 获取日排行榜（前20名），返回带专注时长的完整数据
     */
    @Override
    public Result getDailyRank() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return getRankWithScores(RANK_FOCUS_DAILY_KEY + today);
    }

    /**
     * 获取周排行榜（前20名），返回带专注时长的完整数据
     */
    @Override
    public Result getWeeklyRank() {
        return getRankWithScores(RANK_FOCUS_WEEKLY_KEY + getWeekKey());
    }

    /**
     * 获取总排行榜（前20名），返回带专注时长的完整数据
     */
    @Override
    public Result getTotalRank() {
        return getRankWithScores(RANK_FOCUS_TOTAL_KEY);
    }

    /**
     * 从 ZSet 中获取排行榜数据（前20名），返回带 score 的完整列表
     * <p>
     * 每个元素包含 userId 和 score（专注时长，单位：分钟）
     */
    private Result getRankWithScores(String key) {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> rankWithScores =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 19);
        if (rankWithScores == null || rankWithScores.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : rankWithScores) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("userId", tuple.getValue());
            item.put("score", tuple.getScore() != null ? tuple.getScore().longValue() : 0);
            result.add(item);
        }
        return Result.ok(result);
    }


    /**
     * 签到
     * <p>
     * 使用 Redis BitMap 记录当月签到情况。
     */
    @Override
    public Result signIn(Long userId) {
        LocalDate now = LocalDate.now();
        String key = USER_SIGN_KEY + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int dayOfMonth = now.getDayOfMonth();
        Boolean isSigned = stringRedisTemplate.opsForValue().getBit(key, dayOfMonth - 1);
        if (Boolean.TRUE.equals(isSigned)) {
            return Result.fail("今日已签到");
        }
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok("签到成功");
    }

    /**
     * 查询签到状态
     * <p>
     * 返回今日是否已签到以及连续签到天数。
     */
    @Override
    public Result getSignStatus(Long userId) {
        LocalDate now = LocalDate.now();
        String key = USER_SIGN_KEY + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int dayOfMonth = now.getDayOfMonth();

        // 检查今天是否已签到
        Boolean todaySigned = stringRedisTemplate.opsForValue().getBit(key, dayOfMonth - 1);
        boolean signed = Boolean.TRUE.equals(todaySigned);

        // 计算连续签到天数（从今天往前数）
        int continuousDays = 0;
        for (int i = dayOfMonth - 1; i >= 0; i--) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, i);
            if (Boolean.TRUE.equals(bit)) {
                continuousDays++;
            } else {
                break;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("signed", signed);
        data.put("continuousDays", continuousDays);
        return Result.ok(data);
    }

    /**
     * 获取当前周标识（用于周排行榜 key）
     * <p>
     * 格式：yyyy-Ww（如 2026-W21）
     */
    private String getWeekKey() {
        LocalDate now = LocalDate.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy")) + "-W" + now.get(java.time.temporal.WeekFields.ISO.weekOfYear());
    }
}
