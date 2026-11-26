package io.micronaut.mcp.conf.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerInfoConfigurationWarningTest {

    @Test
    void warnsOnceWhenOnlyTheInstructionsAreSet() {
        assertWarnsOnce("instructions");
    }

    @Test
    void warnsOnceWhenTheVersionIsMissing() {
        assertWarnsOnce("name");
    }

    @Test
    void warnsOnceWhenTheNameIsMissing() {
        assertWarnsOnce("version");
    }

    private static void assertWarnsOnce(String property) {
        List<ILoggingEvent> warnings = warnings(Map.of("micronaut.mcp.server.info." + property, "value"));
        assertEquals(1, warnings.size());
        assertEquals(Level.WARN, warnings.get(0).getLevel());
        String message = warnings.get(0).getFormattedMessage();
        assertTrue(message.contains("micronaut.mcp.server.info.name") && message.contains("micronaut.mcp.server.info.version"), message);
    }

    @Test
    void doesNotWarnWhenNameAndVersionAreSet() {
        assertEquals(List.of(), warnings(Map.of("micronaut.mcp.server.info.name", "weather", "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.info.instructions", "Ask for a city first.")));
    }

    @Test
    void doesNotWarnWithoutServerInfoProperties() {
        assertEquals(List.of(), warnings(Map.of()));
    }

    @Test
    void groovyStyleCamelCaseIconKeysBind() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.server.info.name", "weather",
            "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.info.icons", List.of(Map.of("src", "https://example.com/weather.png", "mimeType", "image/png"))))) {
            assertEquals("image/png", context.getBean(McpServerInfoConfiguration.class).getIcons().get(0).getMimeType());
        }
    }

    private static List<ILoggingEvent> warnings(Map<String, Object> properties) {
        Logger logger = (Logger) LoggerFactory.getLogger(McpServerInfoConfigurationWarning.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try (ApplicationContext context = ApplicationContext.run(properties)) {
            return List.copyOf(appender.list);
        } finally {
            logger.detachAppender(appender);
        }
    }
}
