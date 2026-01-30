package com.hotel.hotelservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    @Value("${service.auth.api-key}")
    private String validApiKey;

    private static final String API_KEY_HEADER = "X-Service-API-Key";
    private static final String SERVICE_USERNAME = "booking-service";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (validApiKey != null && validApiKey.equals(apiKey)) {
            System.out.println("DEBUG: Valid service API key received");

            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_SERVICE"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            var authentication = new UsernamePasswordAuthenticationToken(
                    SERVICE_USERNAME,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("DEBUG: Service authentication set for: " + SERVICE_USERNAME);
        } else {
            System.out.println("DEBUG: No valid service API key. API key provided: " +
                    (apiKey != null ? "yes" : "no"));
        }

        filterChain.doFilter(request, response);
    }
}