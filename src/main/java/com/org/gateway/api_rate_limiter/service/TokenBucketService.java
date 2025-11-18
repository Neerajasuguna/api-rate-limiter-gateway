package com.org.gateway.api_rate_limiter.service;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class TokenBucketService {

    private final RedisCommands<String, String> redis;
    private final String luaScript;

    public TokenBucketService(RedisCommands<String, String> redis) throws IOException {
        this.redis = redis;
        this.luaScript = loadLua();
    }

    private String loadLua() throws IOException {
        ClassPathResource resource = new ClassPathResource("redis/token_bucket.lua");
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * @param key Redis key for bucket
     * @param capacity Max tokens in bucket
     * @param refillRate tokens added per second
     */
    public boolean isAllowed(String key, long capacity, long refillRate) {
        long now = System.currentTimeMillis();

        Object result = redis.eval(
                luaScript,
                ScriptOutputType.INTEGER,
                new String[]{key},
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(now)
        );

        return Integer.parseInt(result.toString()) == 1;
    }
}
