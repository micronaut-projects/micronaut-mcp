package example.micronaut;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.modelcontextprotocol.server.McpSyncServer;
import picocli.CommandLine.Command;
import jakarta.inject.Inject;

@Command(name = "pgn-resources", description = "A MCP Server using stdio transport which exposes PGN files as MCP Server resources")
public class McpServerCommand implements Runnable {
    private volatile boolean running = true;
    @Inject
    McpSyncServer mcpServer;

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            ex.printStackTrace(System.err); // Force to STDERR
        });

        PicocliRunner.run(McpServerCommand.class,
            ApplicationContext.builder().banner(false).build(),
            args);
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
