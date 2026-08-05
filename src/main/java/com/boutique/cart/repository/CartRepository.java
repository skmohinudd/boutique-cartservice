package com.boutique.cart.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class CartRepository {

    private static final String KEY_PREFIX = "cart:";

    private static final DefaultRedisScript<Long> ADD_ITEM_SCRIPT =
            new DefaultRedisScript<>("""
                    local current = tonumber(redis.call('HGET', KEYS[1], ARGV[1]) or '0')
                    local increment = tonumber(ARGV[2])
                    local maximum = tonumber(ARGV[3])
                    local updated = current + increment

                    if updated > maximum then
                      return -1
                    end

                    redis.call('HSET', KEYS[1], ARGV[1], tostring(updated))
                    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
                    return updated
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration cartTtl;
    private final int maximumQuantity;

    public CartRepository(
            StringRedisTemplate redisTemplate,
            @Value("${cart.ttl}") Duration cartTtl,
            @Value("${cart.maximum-quantity}") int maximumQuantity
    ) {
        this.redisTemplate = redisTemplate;
        this.cartTtl = cartTtl;
        this.maximumQuantity = maximumQuantity;
    }

    public int addQuantity(UUID userId, UUID productId, int increment) {
        Long result = redisTemplate.execute(
                ADD_ITEM_SCRIPT,
                Collections.singletonList(key(userId)),
                productId.toString(),
                Integer.toString(increment),
                Integer.toString(maximumQuantity),
                Long.toString(cartTtl.toSeconds())
        );

        if (result == null) {
            throw new IllegalStateException("Redis did not return a cart update result.");
        }

        return result.intValue();
    }

    public void setQuantity(UUID userId, UUID productId, int quantity) {
        redisTemplate.opsForHash().put(key(userId), productId.toString(), Integer.toString(quantity));
        redisTemplate.expire(key(userId), cartTtl);
    }

    public Map<UUID, Integer> findItems(UUID userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
        Map<UUID, Integer> result = new LinkedHashMap<>();

        entries.forEach((field, value) ->
                result.put(
                        UUID.fromString(field.toString()),
                        Integer.parseInt(value.toString())
                )
        );

        return result;
    }

    public boolean removeItem(UUID userId, UUID productId) {
        return redisTemplate.opsForHash().delete(key(userId), productId.toString()) > 0;
    }

    public void clear(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
