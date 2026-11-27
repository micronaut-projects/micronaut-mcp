package io.micronaut.mcp.server.stdio.sync;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.stdio.StdioTestTransportProviders;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.time.Duration;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_INITIALIZATION_2025_WITH_LOGGING;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;

@Property(name = "micronaut.mcp.server.info.name", value="mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "spec.name", value = "StdioResourcesListNoResourcesCapabilityTest")
@MicronautTest
class StdioResourcesListNoResourcesCapabilityTest {
    public static final String RESOURCES_LIST = """
        {"jsonrpc": "2.0","id": 1,"method":"resources/list"}""";
    public static final String RESOURCES_LIST_RESULT = """
{"jsonrpc": "2.0","id": 1,"result": {"resources":[]}}""";

    @Inject
    ReplacementMcpServerTransportProviderFactory factory;

    @Test
    void listsNoResources() throws IOException, InterruptedException, JSONException {
        factory.stdio.sendRequest(INITIALIZE);
        Duration timeout = Duration.ofSeconds(10);
        JSONAssert.assertEquals(EXPECTED_INITIALIZATION_2025_WITH_LOGGING, factory.stdio.readResponse(timeout), true);
        factory.stdio.sendRequest(INITIALIZED);
        factory.stdio.sendRequest(RESOURCES_LIST);
        JSONAssert.assertEquals(RESOURCES_LIST_RESULT, factory.stdio.readResponse(timeout), true);
    }

    @Requires(property = "spec.name", value = "StdioResourcesListNoResourcesCapabilityTest")
    @Factory
    static class ReplacementMcpServerTransportProviderFactory implements AutoCloseable {
        public final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(McpJsonMapper jsonMapper) {
            return StdioTestTransportProviders.gracefulListing(jsonMapper, stdio);
        }

        @PreDestroy
        @Override
        public void close() throws IOException {
            stdio.close();
        }
    }
}
