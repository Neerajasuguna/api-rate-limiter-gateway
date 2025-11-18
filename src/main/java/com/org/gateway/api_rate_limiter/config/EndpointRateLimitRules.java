package com.org.gateway.api_rate_limiter.config;

import java.util.Map;

public class EndpointRateLimitRules {

    public static class EndpointRule {
        public long windowMs;
        public long limit;

        public EndpointRule(long windowMs, long limit) {
            this.windowMs = windowMs;
            this.limit = limit;
        }
    }

    // Configure rate limit per endpoint (method + path)
    public static final Map<String, EndpointRule> rules = Map.of(
            "GET:/public/data",        new EndpointRule(10000, 100),
            "GET:/search",             new EndpointRule(10000, 20),
            "POST:/payments/charge",   new EndpointRule(10000, 5),
            "GET:/test",               new EndpointRule(10000, 10)  // For testing
    );

    public static EndpointRule getRule(String method, String path) {
        return rules.get(method + ":" + path);
    }
}
