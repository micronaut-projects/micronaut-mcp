package io.micronaut.mcp.client.langchain4j.stdio;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StdioConnectionTest {

    @Test
    void aConnectionWithoutCommandIsRejectedWithItsProperty() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.client.stdio.empty.log-events", "true"))) {
            ConfigurationException e = configurationException(assertThrows(RuntimeException.class,
                () -> context.getBean(McpClient.class, Qualifiers.byName("empty"))));
            assertTrue(e.getMessage().contains("micronaut.mcp.client.stdio.empty.command"), e.getMessage());
        }
    }

    @Test
    void anHttpAndAStdioConnectionCannotHaveTheSameName() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.client.http.dup.url", "http://localhost:1/mcp",
            "micronaut.mcp.client.stdio.dup.command", "java"))) {
            ConfigurationException e = configurationException(assertThrows(RuntimeException.class,
                () -> context.getBean(McpToolProvider.class)));
            assertTrue(e.getMessage().contains("[dup]"), e.getMessage());
        }
    }

    @Test
    void theDeprecatedCommandsCannotBeCombinedWithAConnectionNamedStdio() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "langchain4j.mcp.client.transport.stdio.commands", "java",
            "micronaut.mcp.client.stdio.stdio.command", "java"))) {
            ConfigurationException e = configurationException(assertThrows(RuntimeException.class,
                () -> context.getBean(McpToolProvider.class)));
            assertTrue(e.getMessage().contains("[stdio]"), e.getMessage());
        }
    }

    private static ConfigurationException configurationException(Throwable error) {
        Throwable cause = error;
        while (!(cause instanceof ConfigurationException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return assertInstanceOf(ConfigurationException.class, cause);
    }
}
