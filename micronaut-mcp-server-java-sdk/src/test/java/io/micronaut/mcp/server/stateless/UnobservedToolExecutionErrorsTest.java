package io.micronaut.mcp.server.stateless;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.mcp.server.observability.McpServerObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@link ToolExecutionErrorsTest} without observers, as for an application without Micrometer, since the server skips
 * observation entirely then and runs the synchronous tool calls on a path of its own.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolExecutionErrorsTest")
@Property(name = "tool-errors.tools", value = "true")
@Property(name = "micronaut.mcp.server.metrics.enabled", value = "false")
@MicronautTest
class UnobservedToolExecutionErrorsTest extends ToolExecutionErrorsTest {

    @Inject
    BeanContext beanContext;

    @Test
    void nothingObservesTheOperations() {
        assertTrue(beanContext.getBeansOfType(McpServerObserver.class).isEmpty());
    }
}
