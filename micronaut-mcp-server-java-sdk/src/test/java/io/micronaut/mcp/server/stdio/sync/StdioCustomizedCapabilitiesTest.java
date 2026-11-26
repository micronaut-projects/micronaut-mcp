package io.micronaut.mcp.server.stdio.sync;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A STDIO server keeps every capability a {@link BeanCreatedEventListener} of the capabilities builder sets, notifications
 * included, and the capabilities its primitive beans advertise.
 */
@Property(name = "spec.name", value = "StdioCustomizedCapabilitiesTest")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@MicronautTest(startApplication = false)
class StdioCustomizedCapabilitiesTest {

    private static final Map<String, Object> EXPERIMENTAL = Map.of("io.example/experiment", Map.of());
    private static final Map<String, Map<String, Object>> EXTENSIONS = Map.of("io.example/feature", Map.of());

    @Inject
    McpSyncServer server;

    @Test
    void customizedCapabilitiesAreKept() {
        McpSchema.ServerCapabilities capabilities = server.getServerCapabilities();
        assertEquals(EXPERIMENTAL, capabilities.experimental());
        assertEquals(EXTENSIONS, capabilities.extensions());
        assertNotNull(capabilities.completions());
        assertEquals(new McpSchema.ServerCapabilities.ToolCapabilities(true), capabilities.tools());
        assertEquals(new McpSchema.ServerCapabilities.PromptCapabilities(true), capabilities.prompts());
        assertEquals(new McpSchema.ServerCapabilities.ResourceCapabilities(true, true), capabilities.resources());
    }

    @Requires(property = "spec.name", value = "StdioCustomizedCapabilitiesTest")
    @Singleton
    static class CapabilitiesCustomizer implements BeanCreatedEventListener<McpSchema.ServerCapabilities.Builder> {
        @Override
        public McpSchema.ServerCapabilities.Builder onCreated(BeanCreatedEvent<McpSchema.ServerCapabilities.Builder> event) {
            return event.getBean()
                .experimental(EXPERIMENTAL)
                .extensions(EXTENSIONS)
                .tools(true)
                .prompts(true)
                .resources(true, true);
        }
    }

    @Requires(property = "spec.name", value = "StdioCustomizedCapabilitiesTest")
    @Factory
    static class StdioCustomizedCapabilitiesTestFactory implements AutoCloseable {
        private final Stdio stdio = new Stdio();

        @Singleton
        McpServerFeatures.SyncCompletionSpecification completion() {
            return new McpServerFeatures.SyncCompletionSpecification(new McpSchema.PromptReference("greeting"),
                (exchange, request) -> new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(List.of())));
        }

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(McpJsonMapper jsonMapper) {
            return new StdioServerTransportProvider(jsonMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() {
            stdio.close();
        }
    }
}
