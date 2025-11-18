package com.org.gateway.api_rate_limiter.controller;

import com.org.gateway.api_rate_limiter.service.SlidingWindowCounterService;
import com.org.gateway.api_rate_limiter.service.SlidingWindowService;
import com.org.gateway.api_rate_limiter.service.TokenBucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/rate-limiter")
public class TestController {

    private final SlidingWindowService sliding;

    private final TokenBucketService tokenBucketService;
    private final SlidingWindowCounterService slidingWindowCounterService;

    public TestController(SlidingWindowService sliding, TokenBucketService tokenBucketService, SlidingWindowCounterService slidingWindowCounterService) {
        this.sliding = sliding;
        this.tokenBucketService=tokenBucketService;
        this.slidingWindowCounterService = slidingWindowCounterService;

    }

    @GetMapping("/sliding-log")
    public Map<String, Object> testLimit(@RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        if (apiKey == null || apiKey.isBlank()) apiKey = "anonymous";

        String redisKey = "sw:user:" + apiKey;
        long windowMs = 60_000L; // 1 minute
        long limit = 5L;         // allow 5 requests per minute

        boolean allowed = sliding.isAllowed(redisKey, windowMs, limit);

        return Map.of(
                "apiKey", apiKey,
                "allowed", allowed
        );
    }

    @GetMapping("/token-bucket")
    public Map<String, Object> tokenTest(@RequestHeader(value = "X-Api-Key", required = false) String apiKey) {

        if (apiKey == null) apiKey = "anonymous";

        boolean allowed = tokenBucketService.isAllowed(
                "tb:" + apiKey,
                10,  // capacity = 10 tokens
                2    // refill rate = 2 tokens/sec
        );

        return Map.of(
                "apiKey", apiKey,
                "allowed", allowed
        );
    }

    @GetMapping("/sliding-counter")
    public Map<String, Object> counterTest(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey) {

        if (apiKey == null) apiKey = "anon";

        boolean allowed = slidingWindowCounterService.isAllowed(
                "swc:" + apiKey,
                5,         // limit
                60000      // 1 minute window in ms
        );

        return Map.of(
                "apiKey", apiKey,
                "allowed", allowed
        );
    }

//    @GetMapping("/ping-test")
//    public Map<String, String> health() {
//        return Map.of("status", "UP");
//    }

}
