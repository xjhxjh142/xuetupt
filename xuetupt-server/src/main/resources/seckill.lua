-- 秒杀 Lua 脚本
-- KEYS 不使用，参数通过 ARGV 传入
-- ARGV[1]: 库存 key (seckill:stock:courseId)
-- ARGV[2]: 订单 key (seckill:order:courseId)
-- ARGV[3]: 用户 ID

local stockKey = ARGV[1]
local orderKey = ARGV[2]
local userId = ARGV[3]

-- 1. 判断用户是否已预约
local isOrdered = redis.call('SISMEMBER', orderKey, userId)
if isOrdered == 1 then
    return 2  -- 已预约
end

-- 2. 判断库存
local stock = redis.call('GET', stockKey)
if not stock or tonumber(stock) <= 0 then
    return 0  -- 库存不足
end

-- 3. 扣减库存
redis.call('DECR', stockKey)

-- 4. 记录用户
redis.call('SADD', orderKey, userId)

return 1  -- 成功
