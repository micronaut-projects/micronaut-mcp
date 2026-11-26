package io.micronaut.mcp.client.langchain4j.multi;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.health.HealthStatus;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.mcpHealth;
import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.toolNames;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the STDIO connection that the deprecated {@code langchain4j.mcp.client.transport.stdio.commands} property
 * declares, end to end, and that the process of a STDIO server stops with the application.
 */
class LegacyStdioCommandsTest {

    @Test
    void theDeprecatedCommandsDeclareAWorkingConnectionWhoseProcessStopsWithTheApplication() {
        List<String> command = List.of(ProcessHandle.current().info().command().orElse("java"),
            "-Dlogback.configurationFile=logback-stderr.xml", "-cp", System.getProperty("java.class.path"), StdioServer.class.getName());
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < command.size(); i++) {
            properties.put("langchain4j.mcp.client.transport.stdio.commands[" + i + "]", command.get(i));
        }
        // As before named connections, the deprecated commands only declare a connection with the STDIO transport
        properties.put("micronaut.mcp.client.transport", "STDIO");
        Set<Long> childrenBefore = children();
        ProcessHandle server;
        try (ApplicationContext context = ApplicationContext.run(properties)) {
            McpClient client = context.getBean(McpClient.class, Qualifiers.byName("stdio"));
            assertSame(client, context.getBean(McpClient.class, Qualifiers.byName("stdio")), "The client is a singleton");
            assertEquals("stdio", client.key());
            assertEquals(Set.of("hello", "env"), toolNames(context.getBean(McpToolProvider.class)));
            HealthResult health = mcpHealth(List.copyOf(context.getBeansOfType(HealthIndicator.class)));
            assertEquals(HealthStatus.UP, health.getStatus());
            assertEquals(Map.of("stdio", Map.of("status", "UP")), health.getDetails());
            List<ProcessHandle> started = ProcessHandle.current().children()
                .filter(process -> !childrenBefore.contains(process.pid()))
                .toList();
            assertEquals(1, started.size(), "One server process is started for the connection");
            server = started.getFirst();
            assertTrue(server.isAlive());
        }
        ProcessHandle stopped = server;
        assertDoesNotThrow(() -> stopped.onExit().get(10, TimeUnit.SECONDS), "The server process stops with the application");
        assertFalse(server.isAlive());
    }

    private static Set<Long> children() {
        return Set.copyOf(ProcessHandle.current().children().map(ProcessHandle::pid).toList());
    }
}
