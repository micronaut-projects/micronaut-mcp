package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A stateless server turns off the notifications set by a {@link BeanCreatedEventListener} of the capabilities builder,
 * and keeps everything else, whether the listener sets it or a primitive bean advertises it.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "StatelessCustomizedCapabilitiesTest")
@MicronautTest
class StatelessCustomizedCapabilitiesTest {

    private static final Map<String, Object> EXPERIMENTAL = Map.of("io.example/experiment", Map.of());
    private static final Map<String, Map<String, Object>> EXTENSIONS = Map.of("io.example/feature", Map.of());

    @Inject
    McpStatelessSyncServer server;

    @Test
    void customizedCapabilitiesKeepEverythingButNotifications() {
        McpSchema.ServerCapabilities capabilities = server.getServerCapabilities();
        assertEquals(EXPERIMENTAL, capabilities.experimental());
        assertEquals(EXTENSIONS, capabilities.extensions());
        assertNotNull(capabilities.logging());
        assertNotNull(capabilities.completions());
        assertEquals(new McpSchema.ServerCapabilities.ToolCapabilities(false), capabilities.tools());
        assertEquals(new McpSchema.ServerCapabilities.PromptCapabilities(false), capabilities.prompts());
        assertEquals(new McpSchema.ServerCapabilities.ResourceCapabilities(false, false), capabilities.resources());
    }

    @Requires(property = "spec.name", value = "StatelessCustomizedCapabilitiesTest")
    @Singleton
    static class CapabilitiesCustomizer implements BeanCreatedEventListener<McpSchema.ServerCapabilities.Builder> {
        @Override
        public McpSchema.ServerCapabilities.Builder onCreated(BeanCreatedEvent<McpSchema.ServerCapabilities.Builder> event) {
            return event.getBean()
                .experimental(EXPERIMENTAL)
                .logging()
                .extensions(EXTENSIONS)
                .tools(true)
                .prompts(true)
                .resources(true, true);
        }
    }

    @Requires(property = "spec.name", value = "StatelessCustomizedCapabilitiesTest")
    @Factory
    static class CompletionFactory {
        @Singleton
        McpStatelessServerFeatures.SyncCompletionSpecification completion() {
            return new McpStatelessServerFeatures.SyncCompletionSpecification(new McpSchema.PromptReference("greeting"),
                (context, request) -> new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(List.of())));
        }
    }
}
