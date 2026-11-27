package io.micronaut.mcp.client.javasdk.multi;

import java.util.concurrent.CountDownLatch;

/**
 * A process that never answers, started by {@link ClientInitializationFailureTest} as an MCP server over STDIO.
 */
public final class HangingServer {
    private HangingServer() {
    }

    // The arguments of a main method
    @SuppressWarnings("java:S1172")
    public static void main(String[] args) throws InterruptedException {
        new CountDownLatch(1).await();
    }
}
