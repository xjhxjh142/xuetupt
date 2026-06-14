package com.xuetupt.utils;

import java.util.concurrent.TimeUnit;

public class RedisConstants {
    // ========== 登录 ==========
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final Long LOGIN_TOKEN_TTL = 30L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    // ========== 缓存通用 ==========
    public static final Long CACHE_NULL_TTL = 2L;
    public static final Long CACHE_TTL_RANDOM_MIN = -5L;
    public static final Long CACHE_TTL_RANDOM_MAX = 5L;

    // ========== 课程缓存 ==========
    public static final Long CACHE_COURSE_TTL = 30L;
    public static final String CACHE_COURSE_KEY = "cache:course:";
    public static final String CACHE_COURSE_TYPE_KEY = "cache:course:type:";

    // ========== 资料缓存 ==========
    public static final Long CACHE_MATERIAL_TTL = 30L;
    public static final String CACHE_MATERIAL_KEY = "cache:material:";

    // ========== 教师缓存 ==========
    public static final Long CACHE_TEACHER_TTL = 30L;
    public static final String CACHE_TEACHER_KEY = "cache:teacher:";

    // ========== 秒杀 ==========
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String SECKILL_ORDER_KEY = "seckill:order:";
    public static final String LOCK_ORDER_KEY = "lock:order:";

    // ========== 自习室 ==========
    public static final String ROOM_ONLINE_KEY = "room:online:";
    public static final String FOCUS_START_KEY = "focus:start:";
    public static final String USER_SIGN_KEY = "sign:";
    public static final String RANK_FOCUS_DAILY_KEY = "rank:focus:daily:";
    public static final String RANK_FOCUS_WEEKLY_KEY = "rank:focus:weekly:";
    public static final String RANK_FOCUS_TOTAL_KEY = "rank:focus:total";

    // ========== 资料下载 ==========
    public static final String DOWNLOAD_TOKEN_KEY = "download:token:";
    public static final Long DOWNLOAD_TOKEN_TTL = 30L;

    // ========== 分布式锁 ==========
    public static final Long LOCK_SHOP_TTL = 10L;
    public static final Long CACHE_DELAY_DELETE_TIME = 500L;
    public static final TimeUnit CACHE_DELAY_DELETE_UNIT = TimeUnit.MILLISECONDS;

    // ========== 限流 ==========
    public static final String FLOW_LIMIT_KEY = "flow:limit:";
    public static final Long FLOW_LIMIT_WINDOW_SIZE = 5L;
    public static final Long FLOW_LIMIT_MAX_COUNT = 10L;

    // ========== 热点数据预热 ==========
    public static final String HOT_COURSE_KEY = "hot:course:";
    public static final String HOT_MATERIAL_KEY = "hot:material:";
    public static final int HOT_DATA_LOAD_SIZE = 100;

    // ========== RabbitMQ ==========
    public static final String TOPIC_ORDER = "order-topic";
    public static final String TOPIC_DELAY = "delay-topic";
    public static final String TOPIC_LOG = "log-topic";
    public static final String TOPIC_MATERIAL = "material-topic";
}
