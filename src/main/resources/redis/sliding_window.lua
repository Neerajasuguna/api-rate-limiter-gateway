-- KEYS[1] = key
-- ARGV[1] = now_ms
-- ARGV[2] = window_ms
-- ARGV[3] = limit

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local oldest = now - window

-- remove old entries
redis.call('ZREMRANGEBYSCORE', key, 0, oldest)

-- count entries in window
local count = redis.call('ZCARD', key)

if count >= limit then
    return 0
else
    -- add current timestamp as both score and member (unique)
    redis.call('ZADD', key, now, tostring(now))
    -- set expire slightly longer than window
    redis.call('PEXPIRE', key, window + 1000)
    return 1
end
