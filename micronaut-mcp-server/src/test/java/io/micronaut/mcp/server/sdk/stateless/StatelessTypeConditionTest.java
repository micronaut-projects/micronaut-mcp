package io.micronaut.mcp.server.sdk.stateless;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatelessTypeConditionTest {

    @Test
    void serverTypeCondition() {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StatelessTypeConditionTest",
            "micronaut.mcp.server.type", "STATELESS_SYNC"
        ))) {
            assertTrue(ctx.containsBean(StatelessTypeConditionBean.class));
        }
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StatelessTypeConditionTest",
            "micronaut.mcp.server.type", "STATELESS_ASYNC"
        ))) {
            assertTrue(ctx.containsBean(StatelessTypeConditionBean.class));
        }
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StatelessTypeConditionTest",
            "micronaut.mcp.server.type", "SYNC"
        ))) {
            assertFalse(ctx.containsBean(StatelessTypeConditionBean.class));
        }
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "StatelessTypeConditionTest",
            "micronaut.mcp.server.type", "ASYNC"
        ))) {
            assertFalse(ctx.containsBean(StatelessTypeConditionBean.class));
        }
    }

    @Requires(property = "spec.name", value = "StatelessTypeConditionTest")
    @Requires(condition = StatelessTypeCondition.class)
    @Singleton
    static class StatelessTypeConditionBean {
    }
}
