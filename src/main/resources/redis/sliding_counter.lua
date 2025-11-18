-- KEYS[1] = previous window key
-- KEYS[2] = current window key
-- ARGV[1] = limit
-- ARGV[2] = now (ms)
-- ARGV[3] = window size in ms

local limit = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local window = tonumber(ARGV[3])

local current_window = math.floor(now / window)
local prev_window = current_window - 1

local elapsed = now % window
local weight = elapsed / window

local prev_count = tonumber(redis.call("GET", KEYS[1])) or 0
local curr_count = tonumber(redis.call("GET", KEYS[2])) or 0

local effective = prev_count * (1 - weight) + curr_count

if effective >= limit then
    return 0
end

redis.call("INCR", KEYS[2])
redis.call("PEXPIRE", KEYS[2], window * 2)
redis.call("PEXPIRE", KEYS[1], window * 2)

return 1
