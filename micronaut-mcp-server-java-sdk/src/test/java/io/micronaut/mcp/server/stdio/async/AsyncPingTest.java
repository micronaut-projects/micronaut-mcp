package io.micronaut.mcp.server.stdio.async;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
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
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PING;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/ping">Ping</a>
 */
@Property(name = "spec.name", value = "AsyncPingTest")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@MicronautTest(startApplication = false)
class AsyncPingTest {

    @Inject
    AsyncInitializeTestFactory factory;

    @SuppressWarnings("java:S2925")
    @Test
    void asyncPing() throws JSONException, IOException, InterruptedException {
        factory.stdio.sendRequest(INITIALIZE);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest("""
            {"jsonrpc": "2.0", "method": "notifications/initialized"}""");
        factory.stdio.sendRequest(PING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        List<String> responses = factory.stdio.readResponses();
        assertEquals(2, responses.size());
        String responseJson = responses.get(1);
        assertNotNull(responseJson);
        JSONAssert.assertEquals(PONG, responseJson, true);
    }

    @Requires(property = "spec.name", value = "AsyncPingTest")
    @Factory
    static class AsyncInitializeTestFactory implements AutoCloseable {
        public final Stdio stdio = new Stdio();

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
