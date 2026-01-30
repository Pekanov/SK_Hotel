package com.hotel.bookingservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Value("${service.auth.api-key}")
    private String apiKey;

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                requestTemplate.header("X-Trace-Id", traceId);
            }

            requestTemplate.header("X-Service-API-Key", apiKey);

            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authHeader = request.getHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        requestTemplate.header("Authorization", authHeader);
                        System.out.println("DEBUG: Forwarding JWT token to hotel-service");
                    } else {
                        System.out.println("DEBUG: No Authorization header found in current request");
                    }
                } else {
                    System.out.println("DEBUG: No request attributes available");
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Error getting Authorization header: " + e.getMessage());
            }
        };
    }
}