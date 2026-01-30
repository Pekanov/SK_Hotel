package com.hotel.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class TraceIdFilter extends AbstractGatewayFilterFactory<TraceIdFilter.Config> {

    public TraceIdFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");

            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }

            final String finalTraceId = traceId;
            MDC.put("traceId", finalTraceId);

            log.debug("Request received with traceId: {}, path: {}",
                    finalTraceId, exchange.getRequest().getPath());

            var modifiedRequest = exchange.getRequest().mutate()
                    .header("X-Trace-Id", finalTraceId)
                    .build();

            var modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange)
                    .doFinally(signalType -> MDC.clear());
        };
    }

    public static class Config {
    }
}