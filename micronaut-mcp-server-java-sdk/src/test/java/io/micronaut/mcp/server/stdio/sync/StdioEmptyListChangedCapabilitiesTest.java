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
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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
    private static final String TOOLS_LIST_CHANGED = "\"method\":\"notifications/tools/list_changed\"";
    private static final String PROMPTS_LIST_CHANGED = "\"method\":\"notifications/prompts/list_changed\"";
    private static final String RESOURCES_LIST_CHANGED = "\"method\":\"notifications/resources/list_changed\"";

    private int requestId;

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
    void addingAndRemovingPrimitivesNotifiesTheClient() throws IOException, InterruptedException, JSONException {
        Stdio stdio = factory.stdio;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdio.serverToClient, StandardCharsets.UTF_8));
        stdio.sendRequest(INITIALIZE);
        readUntil(reader, "\"id\":0");
        stdio.sendRequest(INITIALIZED);

        server.addTool(McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name("added").inputSchema(Map.of("type", "object")).build())
            .callHandler((exchange, request) -> McpSchema.CallToolResult.builder().addTextContent("added at runtime").build())
            .build());
        readUntil(reader, TOOLS_LIST_CHANGED);
        assertListed(stdio, reader, "tools/list", """
            {"tools": [{"name": "added"}]}""");
        server.removeTool("added");
        readUntil(reader, TOOLS_LIST_CHANGED);

        server.addPrompt(new McpServerFeatures.SyncPromptSpecification(new McpSchema.Prompt("greeting", null, null, List.of()),
            (exchange, request) -> new McpSchema.GetPromptResult(null, List.of(), null)));
        readUntil(reader, PROMPTS_LIST_CHANGED);
        assertListed(stdio, reader, "prompts/list", """
            {"prompts": [{"name": "greeting"}]}""");
        server.removePrompt("greeting");
        readUntil(reader, PROMPTS_LIST_CHANGED);

        server.addResource(new McpServerFeatures.SyncResourceSpecification(McpSchema.Resource.builder("file:///added.txt", "added").build(),
            (exchange, request) -> new McpSchema.ReadResourceResult(List.of())));
        readUntil(reader, RESOURCES_LIST_CHANGED);
        assertListed(stdio, reader, "resources/list", """
            {"resources": [{"uri": "file:///added.txt"}]}""");
        server.removeResource("file:///added.txt");
        readUntil(reader, RESOURCES_LIST_CHANGED);

        server.addResourceTemplate(new McpServerFeatures.SyncResourceTemplateSpecification(
            McpSchema.ResourceTemplate.builder("file:///{name}", "files").build(),
            (exchange, request) -> new McpSchema.ReadResourceResult(List.of())));
        readUntil(reader, RESOURCES_LIST_CHANGED);
        assertListed(stdio, reader, "resources/templates/list", """
            {"resourceTemplates": [{"uriTemplate": "file:///{name}"}]}""");
        server.removeResourceTemplate("file:///{name}");
        readUntil(reader, RESOURCES_LIST_CHANGED);
    }

    private void assertListed(Stdio stdio, BufferedReader reader, String method, String expectedResult)
        throws IOException, InterruptedException, JSONException {
        int id = ++requestId;
        stdio.sendRequest("""
            {"jsonrpc": "2.0", "id": %d, "method": "%s", "params": {}}""".formatted(id, method));
        JSONAssert.assertEquals("{\"id\": " + id + ", \"result\": " + expectedResult + "}",
            readUntil(reader, "\"id\":" + id), JSONCompareMode.LENIENT);
    }

    /**
     * Reads the lines the server writes until one contains the given text, on the test thread so the pipe keeps a live
     * reader. Other messages, such as log notifications, are skipped.
     */
    private static String readUntil(BufferedReader reader, String text) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line.contains(text)) {
                    return line;
                }
            } else {
                TimeUnit.MILLISECONDS.sleep(20);
            }
        }
        return fail("The server did not write " + text + " within " + TIMEOUT);
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
