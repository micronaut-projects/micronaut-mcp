package io.micronaut.mcp.server.stateless;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.server.AbstractMcpServerFactory;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A stateless server logs at debug level that it turns off the notifications it was configured to advertise, and logs
 * nothing when there is nothing to turn off.
 */
class StatelessNotificationsLogTest {

    private static final String MESSAGE = "advertises listChanged and subscribe as false";
    private static final String CUSTOMIZER = "spec.customizer";

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

    static Stream<Arguments> configuredNotifications() {
        return Stream.of(false, true).flatMap(reactive -> Stream.of(
                "micronaut.mcp.server.tools.list-changed",
                "micronaut.mcp.server.prompts.list-changed",
                "micronaut.mcp.server.resources.list-changed",
                "micronaut.mcp.server.resources.subscribe")
            .map(property -> Arguments.of(reactive, property)));
    }

    @ParameterizedTest
    @MethodSource("configuredNotifications")
    void configuredNotificationsAreLogged(boolean reactive, String property) {
        Map<String, Object> properties = new HashMap<>(Map.of(property, StringUtils.TRUE));
        if (property.endsWith("subscribe")) {
            // subscribe alone does not advertise the resources capability on a server without resources
            properties.put(CUSTOMIZER, "subscribe");
        }
        List<String> messages = messagesWhileStarting(reactive, properties);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains(MESSAGE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nothingIsLoggedWithoutConfiguredNotifications(boolean reactive) {
        assertEquals(List.of(), messagesWhileStarting(reactive, Map.of()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nothingIsLoggedWhenTheCapabilitiesAlreadyHaveNoNotifications(boolean reactive) {
        assertEquals(List.of(), messagesWhileStarting(reactive, Map.of(CUSTOMIZER, "without-notifications")));
    }

    private List<String> messagesWhileStarting(boolean reactive, Map<String, Object> properties) {
        Map<String, Object> configuration = new HashMap<>(properties);
        configuration.put("micronaut.mcp.server.info.name", "mcp-server");
        configuration.put("micronaut.mcp.server.info.version", "0.0.1");
        configuration.put("micronaut.mcp.server.transport", "HTTP");
        configuration.put("micronaut.mcp.server.reactive", String.valueOf(reactive));
        try (ApplicationContext context = ApplicationContext.run(configuration)) {
            Class<?> server = reactive ? McpStatelessAsyncServer.class : McpStatelessSyncServer.class;
            context.getBean(server);
        }
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Requires(property = CUSTOMIZER, value = "subscribe")
    @Singleton
    static class SubscribeCustomizer implements BeanCreatedEventListener<McpSchema.ServerCapabilities.Builder> {
        @Override
        public McpSchema.ServerCapabilities.Builder onCreated(BeanCreatedEvent<McpSchema.ServerCapabilities.Builder> event) {
            return event.getBean().resources(true, false);
        }
    }

    @Requires(property = CUSTOMIZER, value = "without-notifications")
    @Singleton
    static class WithoutNotificationsCustomizer implements BeanCreatedEventListener<McpSchema.ServerCapabilities.Builder> {
        @Override
        public McpSchema.ServerCapabilities.Builder onCreated(BeanCreatedEvent<McpSchema.ServerCapabilities.Builder> event) {
            return event.getBean().tools(false).prompts(false).resources(false, false);
        }
    }
}
