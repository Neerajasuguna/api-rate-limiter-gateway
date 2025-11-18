package com.org.gateway.api_rate_limiter.gateway;

import com.org.gateway.api_rate_limiter.config.EndpointRateLimitRules;
import com.org.gateway.api_rate_limiter.config.RateLimitRules;
import com.org.gateway.api_rate_limiter.config.UserPlanConfig;
import com.org.gateway.api_rate_limiter.service.SlidingWindowCounterService;
import com.org.gateway.api_rate_limiter.service.SlidingWindowService;
import com.org.gateway.api_rate_limiter.service.TokenBucketService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final SlidingWindowService slidingWindow;
    private final TokenBucketService tokenBucket;
    private final SlidingWindowCounterService counter;
    private final UserPlanConfig userPlanConfig;

    // Prometheus metrics
    private final Counter endpointBlocked;
    private final Counter ipBlocked;
    private final Counter userBlocked;
    private final Counter globalBlocked;
    private final Counter allowedRequests;

    public RateLimitInterceptor(
            SlidingWindowService slidingWindow,
            TokenBucketService tokenBucket,
            SlidingWindowCounterService counter,
            UserPlanConfig userPlanConfig,
            MeterRegistry registry) {

        this.slidingWindow = slidingWindow;
        this.tokenBucket = tokenBucket;
        this.counter = counter;
        this.userPlanConfig = userPlanConfig;

        this.endpointBlocked = Counter.builder("rate_limit_block_endpoint_total")
                .description("Requests blocked due to endpoint-specific limits")
                .register(registry);

        this.ipBlocked = Counter.builder("rate_limit_block_ip_total")
                .description("Requests blocked due to IP rate limits")
                .register(registry);

        this.userBlocked = Counter.builder("rate_limit_block_user_total")
                .description("Requests blocked due to user token bucket limits")
                .register(registry);

        this.globalBlocked = Counter.builder("rate_limit_block_global_total")
                .description("Requests blocked due to global rate limits")
                .register(registry);

        this.allowedRequests = Counter.builder("rate_limit_allowed_total")
                .description("Requests allowed after passing all checks")
                .register(registry);
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {

        // --- MDC Logging Context ---
        String apiKey = req.getHeader("X-Api-Key");
        if (apiKey == null || apiKey.isBlank()) apiKey = "anonymous";

        MDC.put("request_id", UUID.randomUUID().toString());
        MDC.put("path", req.getRequestURI());
        MDC.put("method", req.getMethod());
        MDC.put("ip", req.getRemoteAddr());
        MDC.put("apiKey", apiKey);

        String ip = req.getRemoteAddr();
        String method = req.getMethod();
        String path = req.getRequestURI();

        log.info(">>> Incoming request method={} path={} apiKey={} ip={}",
                method, path, apiKey, ip);

        // =============================
        // 1️⃣ ENDPOINT-SPECIFIC LIMITS
        // =============================

        EndpointRateLimitRules.EndpointRule epRule =
                EndpointRateLimitRules.getRule(method, path);

        if (epRule != null) {
            boolean epAllowed = slidingWindow.isAllowed(
                    "ep:" + method + ":" + path,
                    epRule.windowMs,
                    epRule.limit
            );

            long epRemaining = epAllowed ? (epRule.limit - 1) : 0;
            long epReset = (System.currentTimeMillis() + epRule.windowMs) / 1000;

            addRateLimitHeaders(res, epRule.limit, epRemaining, epReset);

            if (!epAllowed) {
                endpointBlocked.increment();
                send429(res, "Endpoint rate limit exceeded", epReset);
                return false;
            }
        }

        // =============================
        // 2️⃣ USER PLAN RESOLUTION
        // =============================

        UserPlanConfig.Plan plan = userPlanConfig.getPlan(apiKey);
        RateLimitRules.Rule planRules = RateLimitRules.rules.get(plan);

        log.info("User plan: apiKey={} plan={}", apiKey, plan);

        // =============================
        // 3️⃣ PER-IP SLIDING WINDOW
        // =============================

        boolean ipAllowed = slidingWindow.isAllowed(
                "ip:" + ip,
                10_000,
                planRules.ipLimit
        );

        long ipRemaining = ipAllowed ? (planRules.ipLimit - 1) : 0;
        long ipReset = (System.currentTimeMillis() + 10_000) / 1000;

        addRateLimitHeaders(res, planRules.ipLimit, ipRemaining, ipReset);

        if (!ipAllowed) {
            ipBlocked.increment();
            send429(res, "Too many requests from IP", ipReset);
            return false;
        }

        // =============================
        // 4️⃣ USER TOKEN BUCKET
        // =============================

        boolean userAllowed = tokenBucket.isAllowed(
                "tb:" + apiKey,
                planRules.userCapacity,
                planRules.userRefillRate
        );

        long userRemaining = userAllowed ? (planRules.userCapacity - 1) : 0;
        long userReset = (System.currentTimeMillis() + 1000) / 1000;

        addRateLimitHeaders(res, planRules.userCapacity, userRemaining, userReset);

        if (!userAllowed) {
            userBlocked.increment();
            send429(res, "User rate limit exceeded", userReset);
            return false;
        }

        // =============================
        // 5️⃣ GLOBAL SLIDING COUNTER
        // =============================

        boolean globalAllowed = counter.isAllowed(
                "global",
                planRules.globalLimit,
                60_000
        );

        long globalRemaining = globalAllowed ? (planRules.globalLimit - 1) : 0;
        long globalReset = (System.currentTimeMillis() + 60_000) / 1000;

        addRateLimitHeaders(res, planRules.globalLimit, globalRemaining, globalReset);

        if (!globalAllowed) {
            globalBlocked.increment();
            send429(res, "Global rate limit reached", globalReset);
            return false;
        }

        // =============================
        // 6️⃣ FINAL SUCCESS
        // =============================

        log.info(">>> Request ALLOWED ✔");
        allowedRequests.increment();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        MDC.clear();
    }

    // =============================
    // Helper Methods
    // =============================

    private void send429(HttpServletResponse res, String msg, long retryAfterSeconds) throws Exception {
        res.setStatus(429);
        res.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        res.getWriter().write("{\"error\":\"" + msg + "\"}");
    }

    private void addRateLimitHeaders(HttpServletResponse res, long limit, long remaining, long resetEpochSeconds) {
        res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        res.setHeader("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));
    }
}
