-- KEYS: lockKey
-- ARGV: lockerId, syncChannel, value, valueLiveTime
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
local syncChannel = ARGV[2];
local value = ARGV[3];
local valueLiveTime = ARGV[4];
if (valueLiveTime ~= nil) then
    valueLiveTime = tonumber(valueLiveTime);
end
-- 获取owner
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false) then
    return false;
end
-- 尝试解读锁
local success = false;
if (owner == 'writer' or owner == 'reader-writer') then
    -- 获取writer
    local writer = redis.call('hget', lockKey, 'writer');
    if (lockerId == writer) then
        -- 删除writer
        writer = false;
        redis.call('hdel', lockKey, 'writer');
        -- 更新owner
        if (owner == 'writer') then
            owner = 'none';
        else
            owner = 'readers';
        end
        redis.call('hset', lockKey, 'owner', owner);
        -- 判断是否删除key
        if (owner == 'none') then
            -- 如无value，则删除key
            local existingValue = redis.call('hget', lockKey, 'value');
            if (existingValue == false) then
                redis.call('del', lockKey);
            end
        end

        success = true;
    end
end
-- 发布同步消息
redis.call('publish', syncChannel, 0);
-- 如果加锁成功，则设置value
if (success == true and value ~= nil) then
    redis.call('hset', lockKey, 'value', value);
    if (owner == 'none' and valueLiveTime ~= nil) then
        redis.call('pexpire', lockKey, valueLiveTime);
    end
end

return success;
