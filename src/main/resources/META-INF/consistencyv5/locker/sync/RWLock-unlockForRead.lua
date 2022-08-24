-- KEYS: lockKey
-- ARGV: lockerId, currentTime, syncChannel, value, valueLiveTime
-- return: true（成功）；false（失败，锁不存在或已经易主）

-- 数据结构（hash）
-- ${lockKey}:
--   value: ${value}
--   owner: none、writer、readers、reader-writer
--   writerBooking: ${writerBooking}
--   writer: ${lockerId}
--   readerAmount: ${读者数量}
--   readerAmountDeadline: ${readerAmount的存活时间}
--   reader-${lockerId1}: ${readerDeadline1}
--   reader-${lockerId2}: ${readerDeadline2}
--   reader-${lockerId3}: ${readerDeadline3}

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local currentTime = tonumber(ARGV[2]);
local syncChannel = ARGV[3];
local value = ARGV[4];
local valueLiveTime = ARGV[5];
if (valueLiveTime ~= nil) then
    valueLiveTime = tonumber(valueLiveTime);
end
-- 获取owner
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false) then
    return false;
end
-- 获取readerAmount
local readerAmount = redis.call('hget', lockKey, 'readerAmount');
if (readerAmount ~= false) then
    readerAmount = tonumber(readerAmount);
else
    readerAmount = 0;
end
-- 尝试解读锁
local success = false;
if (owner == 'readers' or owner == 'reader-writer') then
    local readerKey = 'reader-' .. lockerId;
    local readerDeadline = redis.call('hget', lockKey, readerKey);
    if (readerDeadline ~= false) then
        -- 删除reader
        readerDeadline = false;
        redis.call('hdel', lockKey, readerKey);
        -- 更新readerAmount
        readerAmount = readerAmount - 1;
        redis.call('hset', lockKey, 'readerAmount', readerAmount);

        success = true;
    end
    if (readerAmount <= 0) then
        -- 更新owner
        if (owner == 'readers') then
            owner = 'none';
            redis.call('hset', lockKey, 'owner', owner);
        else
            owner = 'writer';
            redis.call('hset', lockKey, 'owner', owner);
        end
    end
end
if (owner == 'none') then
    -- 获取writerBooking
    local writerBooking = redis.call('hget', lockKey, 'writerBooking');
    if (writerBooking ~= false) then
        writerBooking = tonumber(writerBooking);
    end
    if (writerBooking == false or writerBooking < currentTime) then
        -- 删除writerBooking
        writerBooking = false;
        redis.call('hdel', lockKey, 'writerBooking');
        -- 如无value，则删除key
        local existingValue = redis.call('hget', lockKey, 'value');
        if (existingValue == false) then
            redis.call('del', lockKey);
        end
    end
end
-- 发布同步消息
if (readerAmount <= 1) then
    redis.call('publish', syncChannel, 0);
end
-- 设置value
if (success == true and value ~= nil) then
    redis.call('hset', lockKey, 'value', value);
    if (owner == 'none' and valueLiveTime ~= nil) then
        redis.call('pexpire', lockKey, valueLiveTime);
    end
end

return success;
