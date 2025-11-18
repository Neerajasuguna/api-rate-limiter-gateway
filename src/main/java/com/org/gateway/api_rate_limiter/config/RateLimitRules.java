package com.org.gateway.api_rate_limiter.config;

import java.util.Map;

public class RateLimitRules {

    public static class Rule {
        public long ipLimit;
        public long userCapacity;
        public long userRefillRate;
        public long globalLimit;

        public Rule(long ipLimit, long userCapacity, long userRefillRate, long globalLimit) {
            this.ipLimit = ipLimit;
            this.userCapacity = userCapacity;
            this.userRefillRate = userRefillRate;
            this.globalLimit = globalLimit;
        }
    }

    public static final Map<UserPlanConfig.Plan, Rule> rules = Map.of(
        UserPlanConfig.Plan.FREE, new Rule(5, 10, 1, 100),
        UserPlanConfig.Plan.PRO, new Rule(20, 50, 5, 500),
        UserPlanConfig.Plan.ENTERPRISE, new Rule(100, 200, 20, 2000)
    );
}
