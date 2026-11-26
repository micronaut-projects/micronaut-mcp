package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The reactive stateless server does not advertise notifications it cannot send either, and keeps the other
 * capabilities a {@link BeanCreatedEventListener} sets.
 */
@Property(name = "spec.name", value = "StatelessAsyncNotificationCapabilitiesTest")
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.tools.list-changed", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.prompts.list-changed", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.resources.list-changed", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.resources.subscribe", value = StringUtils.TRUE)
@MicronautTest
class StatelessAsyncNotificationCapabilitiesTest {

    private static final Map<String, Object> EXPERIMENTAL = Map.of("io.example/experiment", Map.of());

    @Inject
    McpStatelessAsyncServer server;

    @Test
    void aReactiveStatelessServerDoesNotAdvertiseNotificationsItCannotSend() {
        McpSchema.ServerCapabilities capabilities = server.getServerCapabilities();
        assertEquals(new McpSchema.ServerCapabilities.ToolCapabilities(false), capabilities.tools());
        assertEquals(new McpSchema.ServerCapabilities.PromptCapabilities(false), capabilities.prompts());
        assertFalse(capabilities.resources().subscribe());
        assertFalse(capabilities.resources().listChanged());
        assertEquals(EXPERIMENTAL, capabilities.experimental());
        assertNotNull(capabilities.logging());
    }

    @Requires(property = "spec.name", value = "StatelessAsyncNotificationCapabilitiesTest")
    @Singleton
    static class CapabilitiesCustomizer implements BeanCreatedEventListener<McpSchema.ServerCapabilities.Builder> {
        @Override
        public McpSchema.ServerCapabilities.Builder onCreated(BeanCreatedEvent<McpSchema.ServerCapabilities.Builder> event) {
            return event.getBean().experimental(EXPERIMENTAL).logging();
        }
    }
}
