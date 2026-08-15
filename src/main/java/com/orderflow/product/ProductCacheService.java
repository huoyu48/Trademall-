package com.orderflow.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商品详情缓存：key = product:{tenantId}:{productId}。
 * 仅缓存单个商品详情，不缓存列表等复杂查询。
 */
@Service
public class ProductCacheService {

    private static final String KEY_PREFIX = "product:";
    private static final long TTL_SECONDS = 600;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String key(Long tenantId, Long productId) {
        return KEY_PREFIX + tenantId + ":" + productId;
    }

    public ProductDTO get(Long tenantId, Long productId) {
        Object o = redisTemplate.opsForValue().get(key(tenantId, productId));
        if (o == null) {
            return null;
        }
        return objectMapper.convertValue(o, ProductDTO.class);
    }

    public void put(Long tenantId, Long productId, ProductDTO dto) {
        redisTemplate.opsForValue().set(key(tenantId, productId), dto, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void evict(Long tenantId, Long productId) {
        redisTemplate.delete(key(tenantId, productId));
    }
}
