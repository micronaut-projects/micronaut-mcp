package io.micronaut.mcp.client.javasdk.multi;

/**
 * A process that never answers, started by {@link ClientInitializationFailureTest} as an MCP server over STDIO.
 */
public final class HangingServer {
    private HangingServer() {
    }

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }
}
