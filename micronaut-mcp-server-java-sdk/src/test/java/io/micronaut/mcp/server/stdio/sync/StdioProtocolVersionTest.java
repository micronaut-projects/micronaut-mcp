package io.micronaut.mcp.server.stdio.sync;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.stdio.StdioTestTransportProviders;
import io.micronaut.mcp.server.utils.Stdio;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The stdio transport must not narrow the protocol versions the SDK supports: a client asking for any of them is answered
 * with the version it asked for. Delegation of {@code protocolVersions()} itself is covered by
 * {@code GracefulListingServerTransportProviderTest}.
 */
class StdioProtocolVersionTest {

    private static final String INITIALIZE = """
        {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"%s","capabilities":{},"clientInfo":{"name":"test-client","version":"0.0.1"}}}""";

    @ParameterizedTest
    @ValueSource(strings = {"2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25"})
    void initializeIsAnsweredWithTheRequestedProtocolVersion(String protocolVersion) throws JSONException {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "micronaut.mcp.server.info.name", "test mcp server",
            "micronaut.mcp.server.info.version", "0.0.1",
            "micronaut.mcp.server.transport", "STDIO",
            "spec.name", "StdioProtocolVersionTest"))) {
            // creating the server sets the session factory, which starts reading the in-memory stdin
            ctx.getBean(McpSyncServer.class);
            Stdio stdio = ctx.getBean(ProtocolVersionTransportProviderFactory.class).stdio;

            List<String> responses = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
                stdio.sendRequest(INITIALIZE.formatted(protocolVersion));
                return stdio.readResponses();
            });

            assertEquals(1, responses.size());
            assertEquals(protocolVersion, new JSONObject(responses.get(0)).getJSONObject("result").getString("protocolVersion"));
        }
    }

    @Requires(property = "spec.name", value = "StdioProtocolVersionTest")
    @Factory
    static class ProtocolVersionTransportProviderFactory implements AutoCloseable {
        final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProvider(McpJsonMapper jsonMapper) {
            return StdioTestTransportProviders.gracefulListing(jsonMapper, stdio);
        }

        @PreDestroy
        @Override
        public void close() {
            stdio.close();
        }
    }
}
