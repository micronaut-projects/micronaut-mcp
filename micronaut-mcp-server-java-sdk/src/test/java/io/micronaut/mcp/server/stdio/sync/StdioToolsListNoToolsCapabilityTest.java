package io.micronaut.mcp.server.stdio.sync;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.stdio.StdioServerTransportProviderReplacement;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value="test mcp server")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "spec.name", value = "StdioToolsListNoToolsCapabilityTest")
@MicronautTest
class StdioToolsListNoToolsCapabilityTest {
    public static final String TOOLS_LIST = """
        {"jsonrpc": "2.0","id": 1,"method": "tools/list"}""";
    public static final String TOOLS_LIST_RESULT = """
{"jsonrpc": "2.0","id": 1,"result": {"tools":[]}}""";

    @Inject
    ReplacementMcpServerTransportProviderFactory factory;

    /**
     * Even if the server does not have the prompts capabilities, clients may not respect that and send a prompts/list request.
     * In that scenario, the server should return an empty list of prompts
     */
    @SuppressWarnings("java:S2925")
    @Test
    void syncPrompts() throws IOException, InterruptedException, JSONException {
        factory.stdio.sendRequest(INITIALIZE);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(INITIALIZED);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(TOOLS_LIST);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        List<String> responses = factory.stdio.readResponses();
        assertEquals(2, responses.size());
        String readJsonRpc = responses.get(1);
        JSONAssert.assertEquals(TOOLS_LIST_RESULT, readJsonRpc, true);
    }

    @Requires(property = "spec.name", value = "StdioToolsListNoToolsCapabilityTest")
    @Factory
    static class ReplacementMcpServerTransportProviderFactory implements AutoCloseable {
        public final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(McpJsonMapper jsonMapper) {
            return new StdioServerTransportProviderReplacement(jsonMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() throws IOException {
            stdio.close();
        }
    }
}
