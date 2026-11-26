package io.micronaut.mcp.server.stateless;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.server.AbstractMcpServerFactory;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A stateless server logs at debug level that it turns off the notifications it was configured to advertise.
 */
class StatelessNotificationsLogTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AbstractMcpServerFactory.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level level;

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
        logger.setLevel(level);
    }

    @Test
    void configuredNotificationsAreLogged() {
        List<String> messages = messagesWhileStarting(Map.of("micronaut.mcp.server.tools.list-changed", StringUtils.TRUE));
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("advertises listChanged and subscribe as false"));
        assertTrue(messages.get(0).contains("ToolCapabilities[listChanged=true]"));
    }

    @Test
    void nothingIsLoggedWithoutConfiguredNotifications() {
        assertEquals(List.of(), messagesWhileStarting(Map.of()));
    }

    private List<String> messagesWhileStarting(Map<String, Object> properties) {
        Map<String, Object> configuration = new HashMap<>(properties);
        configuration.put("micronaut.mcp.server.info.name", "mcp-server");
        configuration.put("micronaut.mcp.server.info.version", "0.0.1");
        configuration.put("micronaut.mcp.server.transport", "HTTP");
        try (ApplicationContext context = ApplicationContext.run(configuration)) {
            context.getBean(McpStatelessSyncServer.class);
        }
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
