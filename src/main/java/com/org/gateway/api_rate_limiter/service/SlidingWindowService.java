package com.org.gateway.api_rate_limiter.service;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SlidingWindowService {

    private final RedisCommands<String, String> redis;
    private final String luaScript;

    public SlidingWindowService(RedisCommands<String, String> redis) throws IOException {
        this.redis = redis;
        this.luaScript = loadLua();
    }

    private String loadLua() throws IOException {
        ClassPathResource resource = new ClassPathResource("redis/sliding_window.lua");
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Returns true if the request is allowed, false if rate limit exceeded.
     *
     * key: Redis key to track (e.g. "sw:user:alice" or "sw:global")
     * windowMs: sliding window in milliseconds (e.g. 60000 for 1 minute)
     * limit: max allowed events in that window
     */
    public boolean isAllowed(String key, long windowMs, long limit) {
        long now = System.currentTimeMillis();

        Object result = redis.eval(
                luaScript,
                ScriptOutputType.INTEGER,
                new String[]{ key },
                String.valueOf(now),
                String.valueOf(windowMs),
                String.valueOf(limit)
        );

        if (result == null) return false;
        return Integer.parseInt(result.toString()) == 1;
    }
}
