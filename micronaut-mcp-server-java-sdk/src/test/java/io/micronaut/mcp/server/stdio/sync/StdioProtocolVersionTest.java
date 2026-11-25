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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins protocol-version negotiation over the stdio transport as wired by the module (decision: advertise the SDK defaults,
 * no 2024-11-05 pin). Because the SDK stdio provider uses the interface default for {@code protocolVersions()}, this test
 * cannot tell whether the decorator delegates that method or inherits the same default; that delegation is pinned by
 * {@code GracefulListingServerTransportProviderTest}.
 */
class StdioProtocolVersionTest {

    private static final String INITIALIZE = """
        {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"%s","capabilities":{},"clientInfo":{"name":"test-client","version":"0.0.1"}}}""";

    @ParameterizedTest
    @ValueSource(strings = {"2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25"})
    void initializeIsAnsweredWithTheRequestedProtocolVersion(String protocolVersion) throws IOException, JSONException {
        assertEquals(protocolVersion, negotiatedProtocolVersion(protocolVersion));
    }

    /**
     * A client asking for a version the server does not support is answered with the latest supported one.
     */
    @Test
    void unsupportedProtocolVersionIsAnsweredWithTheLatestSupportedVersion() throws IOException, JSONException {
        assertEquals("2025-11-25", negotiatedProtocolVersion("2099-01-01"));
    }

    private static String negotiatedProtocolVersion(String protocolVersion) throws IOException, JSONException {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "micronaut.mcp.server.info.name", "test mcp server",
            "micronaut.mcp.server.info.version", "0.0.1",
            "micronaut.mcp.server.transport", "STDIO",
            "spec.name", "StdioProtocolVersionTest"))) {
            // creating the server sets the session factory, which starts reading the in-memory stdin
            ctx.getBean(McpSyncServer.class);
            Stdio stdio = ctx.getBean(ProtocolVersionTransportProviderFactory.class).stdio;

            stdio.sendRequest(INITIALIZE.formatted(protocolVersion));
            List<String> responses = stdio.readLines(1, Duration.ofSeconds(10));

            assertEquals(1, responses.size());
            return new JSONObject(responses.get(0)).getJSONObject("result").getString("protocolVersion");
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
