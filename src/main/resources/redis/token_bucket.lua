-- KEYS[1] = key
-- ARGV[1] = capacity
-- ARGV[2] = refill_rate_per_sec
-- ARGV[3] = now_ms

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call("HMGET", key, "tokens", "last_refill")
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

if tokens == nil then
    tokens = capacity
    last_refill = now
else
    local elapsed_ms = (now - last_refill)
    local refill = math.floor((elapsed_ms / 1000) * refill_rate)
    tokens = math.min(capacity, tokens + refill)
    last_refill = last_refill + (refill * 1000 / refill_rate)
end

local allowed = 0

if tokens > 0 then
    allowed = 1
    tokens = tokens - 1
end

redis.call("HMSET", key, "tokens", tokens, "last_refill", last_refill)
redis.call("PEXPIRE", key, 60000) -- 1 min expiry for bucket

return allowed
