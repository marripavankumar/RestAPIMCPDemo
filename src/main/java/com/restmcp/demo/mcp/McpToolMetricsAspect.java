package com.restmcp.demo.mcp;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

/** Times every MCP tool call ({@code mcp.tool.calls}, tagged by tool and outcome) and logs one line per call. */
@Aspect
@Component
public class McpToolMetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(McpToolMetricsAspect.class);

    private final MeterRegistry meterRegistry;

    public McpToolMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(tool)")
    public Object timeToolCall(ProceedingJoinPoint joinPoint, McpTool tool) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        try {
            Object result = joinPoint.proceed();
            outcome = "success";
            return result;
        }
        finally {
            long nanos = sample.stop(Timer.builder("mcp.tool.calls")
                    .description("MCP tool invocations")
                    .tag("tool", tool.name())
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            log.info("mcp tool={} outcome={} durationMs={}", tool.name(), outcome, nanos / 1_000_000);
        }
    }
}
