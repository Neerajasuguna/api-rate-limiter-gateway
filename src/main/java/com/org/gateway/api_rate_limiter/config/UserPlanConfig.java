package com.org.gateway.api_rate_limiter.config;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class UserPlanConfig {

    public enum Plan {
        FREE, PRO, ENTERPRISE
    }

    // Hard-coded for now (later we can load from DB)
    private final Map<String, Plan> userPlanMap = Map.of(
            "alice", Plan.FREE,
            "bob", Plan.PRO,
            "charlie", Plan.ENTERPRISE
    );

    public Plan getPlan(String apiKey) {
        return userPlanMap.getOrDefault(apiKey, Plan.FREE);
    }
}
