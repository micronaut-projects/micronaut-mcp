package io.micronaut.mcp.server.stdio;

import io.micronaut.mcp.server.utils.Stdio;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

/**
 * Builds the stdio transport provider the module wires in production, but bound to the in-memory pipes of a {@link Stdio}
 * instead of {@link System#in} and {@link System#out}.
 */
public final class StdioTestTransportProviders {

    private StdioTestTransportProviders() {
    }

    public static McpServerTransportProvider gracefulListing(McpJsonMapper jsonMapper, Stdio stdio) {
        return new GracefulListingServerTransportProvider(
            new StdioServerTransportProvider(jsonMapper, stdio.serverStdin, stdio.serverStdout));
    }
}
