-- 秒杀原子扣减脚本
-- KEYS[1]: 库存 key (e.g. seckill:stock:2053027277855002628)
-- ARGV[1]: 扣减数量
-- 返回: -1 库存键不存在；-2 库存不足；>=0 扣减后剩余值

local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -1
end
local n = tonumber(stock)
local q = tonumber(ARGV[1])
if n < q then
    return -2
end
return redis.call('DECRBY', KEYS[1], q)
