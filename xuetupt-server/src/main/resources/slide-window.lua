-- 滑动窗口限流 Lua 脚本
-- KEYS[1]: 限流key
-- ARGV[1]: 窗口大小（秒）
-- ARGV[2]: 最大请求数

local key = KEYS[1]
local windowSize = tonumber(ARGV[1])
local maxCount = tonumber(ARGV[2])

-- 当前时间戳（毫秒）
local now = redis.call('TIME')
local nowMs = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)

-- 窗口起始时间
local windowStart = nowMs - windowSize * 1000

-- 移除窗口外的旧数据
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- 统计当前窗口内的请求数
local currentCount = redis.call('ZCARD', key)

if currentCount >= maxCount then
    -- 限流
    return 0
end

-- 添加当前请求
redis.call('ZADD', key, nowMs, nowMs)
-- 设置过期时间
redis.call('EXPIRE', key, windowSize + 1)

return 1
