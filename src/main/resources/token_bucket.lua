local currentTimestamp = ARGV[1]
local capacity = ARGV[2]
local refillRate = ARGV[3]
local tokens = redis.call('HGET', KEYS[1], 'tokens')
local lastRefill = redis.call('HGET', KEYS[1], 'lastRefill')

if tokens == nil then
tokens = capacity
lastRefill = currentTimestamp
end

local lastRefillNum = tonumber(lastRefill)
local currentTimestampNum = tonumber(currentTimestamp)
local capacityNum = tonumber(capacity)
local refillRateNum = tonumber(refillRate)
local tokensNum = tonumber(tokens)

local elapsedTimeNum = currentTimestampNum - lastRefillNum
local tokensEarnedNum = refillRateNum * elapsedTimeNum / 1000
local allowed
local newTokens = math.min(capacityNum, tokensNum + tokensEarnedNum)

if newTokens >= 1 then
newTokens = newTokens - 1
allowed = true
else
allowed = false
end

redis.call('HSET', KEYS[1], 'tokens', newTokens)
redis.call('HSET', KEYS[1], 'lastRefill', currentTimestampNum)

if allowed then
return 1
else
return 0
end