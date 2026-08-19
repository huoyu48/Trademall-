package com.orderflow.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁：用于在高并发下单场景下串行化同一商品的库存预占关键区。
 * <p>
 * 设计要点：
 * 1. 获取用 {@code SET key value NX PX ttl} —— 仅当 key 不存在时才能设置成功，天然互斥；
 * 2. 释放用 Lua 脚本校验 value 后删除 —— 保证“只能释放自己持有的锁”，避免误删他人锁；
 * 3. 锁带自动过期，防止持有者崩溃后死锁；
 * 4. 数据库层的原子 UPDATE 仍作为最终防线（双重保险防超卖）。
 */
@Component
public class RedisLockService {

    private final StringRedisTemplate redis;

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    public RedisLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 尝试获取锁，在 waitMillis 内自旋重试，成功后锁自动于 ttlMillis 后过期。
     *
     * @return 锁持有标识（释放时需原样回传）；获取失败返回 null
     */
    public String tryLock(String key, long waitMillis, long ttlMillis) {
        String value = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitMillis;
        do {
            Boolean ok = redis.opsForValue().setIfAbsent(key, value, ttlMillis, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(ok)) {
                return value;
            }
            if (System.currentTimeMillis() >= deadline) {
                return null;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        } while (true);
    }

    /**
     * 仅当锁仍由当前持有者持有时才释放。
     */
    public boolean release(String key, String value) {
        Long result = redis.execute(RELEASE_SCRIPT, Collections.singletonList(key), value);
        return result != null && result == 1L;
    }
}
