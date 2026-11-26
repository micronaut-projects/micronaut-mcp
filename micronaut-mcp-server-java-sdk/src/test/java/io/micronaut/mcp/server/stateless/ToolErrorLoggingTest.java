package io.micronaut.mcp.server.stateless;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callToolError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How tool failures are logged: unexpected failures at WARN with the exception, input errors and mapped protocol errors
 * at DEBUG only.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "tool-errors.tools", value = "true")
@MicronautTest
class ToolErrorLoggingTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(ToolRegistry.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level level;

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @BeforeEach
    void attachAppender() {
        level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(level);
    }

    @Test
    void anUnexpectedFailureIsLoggedAtWarnWithTheException() throws IOException {
        callTool(httpClient, jsonMapper, "failing");
        ILoggingEvent event = single(Level.WARN);
        assertEquals("Tool failing failed: the backend is down", event.getFormattedMessage());
        assertNotNull(event.getThrowableProxy());
        assertEquals(IllegalStateException.class.getName(), event.getThrowableProxy().getClassName());
    }

    @Test
    void aResultThatCannotBeSerializedIsLoggedAtWarnWithTheException() throws IOException {
        callTool(httpClient, jsonMapper, "unserializable");
        ILoggingEvent event = single(Level.WARN);
        assertTrue(event.getFormattedMessage().startsWith("The result of tool unserializable could not be serialized: "), event.getFormattedMessage());
        assertNotNull(event.getThrowableProxy());
    }

    @Test
    void anInputErrorIsLoggedAtDebugOnly() throws IOException {
        callTool(httpClient, jsonMapper, "limited", """
            {"count": 11}""");
        assertEquals(List.of(), events(Level.WARN));
        ILoggingEvent event = single(Level.DEBUG);
        assertEquals("Tool limited failed with an input error: count: must be less than or equal to 10", event.getFormattedMessage());
        assertNotNull(event.getThrowableProxy());
    }

    @Test
    void aMappedProtocolErrorIsLoggedAtDebugOnly() throws IOException {
        callToolError(httpClient, jsonMapper, "mapped", HttpStatus.BAD_REQUEST);
        assertEquals(List.of(), events(Level.WARN));
        ILoggingEvent event = single(Level.DEBUG);
        assertEquals("Tool mapped failed with a mapped protocol error", event.getFormattedMessage());
        assertNotNull(event.getThrowableProxy());
    }

    private ILoggingEvent single(Level eventLevel) {
        List<ILoggingEvent> events = events(eventLevel);
        assertEquals(1, events.size(), events.toString());
        return events.getFirst();
    }

    private List<ILoggingEvent> events(Level eventLevel) {
        return appender.list.stream().filter(e -> e.getLevel() == eventLevel).toList();
    }
}
