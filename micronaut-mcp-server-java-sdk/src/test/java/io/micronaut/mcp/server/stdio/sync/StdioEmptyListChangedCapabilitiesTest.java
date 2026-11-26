package io.micronaut.mcp.server.stdio.sync;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A STDIO server with list-changed enabled and no static primitive advertises the capabilities, with list changes and
 * subscriptions, so primitives can be added at runtime.
 */
@Property(name = "spec.name", value = "StdioEmptyListChangedCapabilitiesTest")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.tools.list-changed", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.prompts.list-changed", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.resources.list-changed", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.resources.subscribe", value = StringUtils.TRUE)
@MicronautTest(startApplication = false)
class StdioEmptyListChangedCapabilitiesTest {

    @Inject
    McpSyncServer server;

    @Test
    void aStdioServerAdvertisesListChangesWithoutStaticPrimitives() {
        McpSchema.ServerCapabilities capabilities = server.getServerCapabilities();
        assertEquals(new McpSchema.ServerCapabilities.ToolCapabilities(true), capabilities.tools());
        assertEquals(new McpSchema.ServerCapabilities.PromptCapabilities(true), capabilities.prompts());
        assertTrue(capabilities.resources().subscribe());
        assertTrue(capabilities.resources().listChanged());
    }

    @Requires(property = "spec.name", value = "StdioEmptyListChangedCapabilitiesTest")
    @Factory
    static class StdioEmptyListChangedCapabilitiesTestFactory implements AutoCloseable {
        private final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(McpJsonMapper jsonMapper) {
            return new StdioServerTransportProvider(jsonMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() throws IOException {
            stdio.close();
        }
    }
}
