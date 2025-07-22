package example.micronaut;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

@Command(name = "mcp-server", description = "A MCP Server using stdio transport")
public class McpServerCommand implements Runnable {
    private volatile boolean running = true;

    @Inject
    McpSyncServer mcpServer;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(McpServerCommand.class, args);
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
        }));
        while (running) {
        }
    }
}
