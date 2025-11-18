package com.org.gateway.api_rate_limiter.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor interceptor;

    @Autowired
    public WebConfig(RateLimitInterceptor interceptor) {
        org.slf4j.LoggerFactory.getLogger(WebConfig.class).info("WebConfig loaded, interceptor registered");
        this.interceptor = interceptor;
    }

    // IMPORTANT for Spring Boot 3.2+ / 3.3+
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setPatternParser(null);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**");
    }
}
