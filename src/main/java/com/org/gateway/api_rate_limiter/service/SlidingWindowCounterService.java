package com.org.gateway.api_rate_limiter.service;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SlidingWindowCounterService {

    private final RedisCommands<String, String> redis;
    private final String luaScript;

    public SlidingWindowCounterService(RedisCommands<String, String> redis) throws IOException {
        this.redis = redis;
        this.luaScript = loadLua();
    }

    private String loadLua() throws IOException {
        ClassPathResource resource = new ClassPathResource("redis/sliding_counter.lua");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    public boolean isAllowed(String keyPrefix, long limit, long windowMs) {
        long now = System.currentTimeMillis();

        long currentWindow = now / windowMs;
        long previousWindow = currentWindow - 1;

        String prevKey = keyPrefix + ":" + previousWindow;
        String currKey = keyPrefix + ":" + currentWindow;

        Object result = redis.eval(
                luaScript,
                ScriptOutputType.INTEGER,
                new String[]{prevKey, currKey},
                String.valueOf(limit),
                String.valueOf(now),
                String.valueOf(windowMs)
        );

        return Integer.parseInt(result.toString()) == 1;
    }
}
