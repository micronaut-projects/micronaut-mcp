package io.micronaut.mcp.client.langchain4j;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks that mistakes in the configuration of the connections fail the startup.
 */
class McpClientConnectionsValidatorTest {

    @Test
    void aStdioConnectionWithoutCommandFailsTheStartup() {
        assertEquals("The MCP STDIO connection empty has no command, set micronaut.mcp.client.stdio.empty.command",
            startupError(Map.of("micronaut.mcp.client.stdio.empty.log-events", "true")));
    }

    @Test
    void theCommandsTypoIsPointedOut() {
        assertEquals("The MCP STDIO connection files has no command, set micronaut.mcp.client.stdio.files.command"
                + " (the property is command, not commands)",
            startupError(Map.of("micronaut.mcp.client.stdio.files.commands", "java")));
    }

    @Test
    void anHttpConnectionWithoutUrlFailsTheStartup() {
        assertEquals("The MCP HTTP connection weather has no url, set micronaut.mcp.client.http.weather.url",
            startupError(Map.of("micronaut.mcp.client.http.weather.log-requests", "true")));
    }

    @Test
    void anHttpAndAStdioConnectionCannotHaveTheSameName() {
        assertEquals("Each MCP connection needs a unique name, but micronaut.mcp.client.http.dup and"
                + " micronaut.mcp.client.stdio.dup have the same name 'dup'. Rename one of them.",
            startupError(Map.of(
                "micronaut.mcp.client.http.dup.url", "http://localhost:1/mcp",
                "micronaut.mcp.client.stdio.dup.command", "java")));
    }

    @Test
    void theDeprecatedCommandsCannotBeCombinedWithAConnectionNamedStdio() {
        assertEquals("Each MCP connection needs a unique name, but langchain4j.mcp.client.transport.stdio.commands and"
                + " micronaut.mcp.client.stdio.stdio have the same name 'stdio'. Rename one of them.",
            startupError(Map.of(
                "langchain4j.mcp.client.transport.stdio.commands", "java",
                "micronaut.mcp.client.stdio.stdio.command", "java")));
    }

    @Test
    void theDeprecatedPropertiesAreWarnedAbout() {
        Logger logger = (Logger) LoggerFactory.getLogger(McpClientConnectionsValidator.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.client.transport", "STDIO",
            "langchain4j.mcp.client.transport.stdio.commands", "java"))) {
            assertEquals(List.of(
                    "micronaut.mcp.client.transport is deprecated and has no effect: the connections of micronaut.mcp.client.http"
                        + " and micronaut.mcp.client.stdio are all used, whatever their transport",
                    "langchain4j.mcp.client.transport.stdio.commands is deprecated: it declares a STDIO connection named stdio,"
                        + " declare it with micronaut.mcp.client.stdio.stdio.command instead"),
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
        } finally {
            logger.detachAppender(appender);
        }
    }

    private static String startupError(Map<String, Object> properties) {
        Throwable cause = assertThrows(RuntimeException.class, () -> ApplicationContext.run(properties).close());
        while (!(cause instanceof ConfigurationException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return assertInstanceOf(ConfigurationException.class, cause).getMessage();
    }
}
