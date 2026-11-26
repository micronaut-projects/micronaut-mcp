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
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A STDIO server with list-changed enabled and no static primitive advertises the capabilities, with list changes and
 * subscriptions, so primitives can be added at runtime, and it notifies the client when they change.
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

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Inject
    McpSyncServer server;

    @Inject
    StdioEmptyListChangedCapabilitiesTestFactory factory;

    @Test
    void aStdioServerAdvertisesListChangesWithoutStaticPrimitives() {
        McpSchema.ServerCapabilities capabilities = server.getServerCapabilities();
        assertEquals(new McpSchema.ServerCapabilities.ToolCapabilities(true), capabilities.tools());
        assertEquals(new McpSchema.ServerCapabilities.PromptCapabilities(true), capabilities.prompts());
        assertTrue(capabilities.resources().subscribe());
        assertTrue(capabilities.resources().listChanged());
    }

    @Test
    void addingAndRemovingAToolNotifiesTheClient() throws IOException, InterruptedException {
        Stdio stdio = factory.stdio;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdio.serverToClient, StandardCharsets.UTF_8));
        stdio.sendRequest(INITIALIZE);
        assertTrue(readLine(reader).contains("\"id\":0"));
        stdio.sendRequest(INITIALIZED);

        server.addTool(McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name("added").inputSchema(Map.of("type", "object")).build())
            .callHandler((exchange, request) -> McpSchema.CallToolResult.builder().addTextContent("added at runtime").build())
            .build());
        assertTrue(readLine(reader).contains("\"method\":\"notifications/tools/list_changed\""));
        stdio.sendRequest("""
            {"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}""");
        assertTrue(readLine(reader).contains("\"name\":\"added\""));

        server.removeTool("added");
        assertTrue(readLine(reader).contains("\"method\":\"notifications/tools/list_changed\""));
    }

    /**
     * Reads the next line the server writes, on the test thread so the pipe keeps a live reader.
     */
    private static String readLine(BufferedReader reader) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!reader.ready()) {
            if (System.nanoTime() > deadline) {
                fail("The server wrote nothing within " + TIMEOUT);
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        return reader.readLine();
    }

    @Requires(property = "spec.name", value = "StdioEmptyListChangedCapabilitiesTest")
    @Factory
    static class StdioEmptyListChangedCapabilitiesTestFactory implements AutoCloseable {
        final Stdio stdio = new Stdio();

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
