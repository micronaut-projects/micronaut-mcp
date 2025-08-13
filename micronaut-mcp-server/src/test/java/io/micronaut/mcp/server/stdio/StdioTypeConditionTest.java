package io.micronaut.mcp.server.stdio;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StdioTypeConditionTest {
    @Test
    void serverTypeCondition() {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StdioTypeConditionTest",
            "micronaut.mcp.server.type", "SYNC"
        ))) {
            assertTrue(ctx.containsBean(StdioTypeConditionTest.StdioTypeConditionBean.class));
        }
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StdioTypeConditionTest",
            "micronaut.mcp.server.type", "ASYNC"
        ))) {
            assertTrue(ctx.containsBean(StdioTypeConditionTest.StdioTypeConditionBean.class));
        }
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StdioTypeConditionTest",
            "micronaut.mcp.server.type", "STATELESS_SYNC"
        ))) {
            assertFalse(ctx.containsBean(StdioTypeConditionTest.StdioTypeConditionBean.class));
        }
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StdioTypeConditionTest",
            "micronaut.mcp.server.type", "STATELESS_ASYNC"
        ))) {
            assertFalse(ctx.containsBean(StdioTypeConditionTest.StdioTypeConditionBean.class));
        }
    }

    @Requires(property = "spec.name", value = "StdioTypeConditionTest")
    @Requires(condition = StdioTypeCondition.class)
    @Singleton
    static class StdioTypeConditionBean {
    }
}
