package io.micronaut.mcp.client.langchain4j.multi;

/**
 * A process that never answers, started by {@link UnreachableServersTest} as an MCP server over STDIO.
 */
public final class HangingServer {
    private HangingServer() {
    }

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }
}
