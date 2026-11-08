package io.micronaut.mcp.server.registry;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourcePriorityTest {

    @Test
    void aPriorityOutOfRangeFailsTheStartup() {
        assertEquals("The priority of resource priority://high is 5.0, but it must be between 0 and 1", startupFailure("ResourcePriorityTest.high"));
    }

    @Test
    void aNaNPriorityFailsTheStartup() {
        assertEquals("The priority of resource priority://nan/{id} is NaN, but it must be between 0 and 1", startupFailure("ResourcePriorityTest.nan"));
    }

    private static String startupFailure(String specName) {
        Map<String, Object> properties = Map.of(
            "micronaut.mcp.server.info.name", "mcp-server",
            "micronaut.mcp.server.info.version", "0.0.1",
            "micronaut.mcp.server.transport", "HTTP",
            "spec.name", specName);
        Throwable error = assertThrows(RuntimeException.class, () -> ApplicationContext.run(properties));
        while (error.getCause() != null && !(error instanceof IllegalStateException)) {
            error = error.getCause();
        }
        return error.getMessage();
    }

    @Requires(property = "spec.name", value = "ResourcePriorityTest.high")
    @Singleton
    static class HighPriority {
        @Resource(uri = "priority://high", priority = 5)
        String high() {
            return "high";
        }
    }

    @Requires(property = "spec.name", value = "ResourcePriorityTest.nan")
    @Singleton
    static class NaNPriority {
        @ResourceTemplate(uriTemplate = "priority://nan/{id}", priority = Double.NaN)
        String nan(String id) {
            return id;
        }
    }
}
