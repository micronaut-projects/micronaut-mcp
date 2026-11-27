package io.micronaut.mcp.client.langchain4j.multi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;

/**
 * A process that never answers, started by {@link UnreachableServersTest} as an MCP server over STDIO. It appends its
 * process id to the file named by the {@value #PID_FILE} system property, so that the test can check that it is stopped.
 */
public final class HangingServer {
    /**
     * The system property that names the file the process ids are appended to.
     */
    static final String PID_FILE = "hanging-server.pid-file";

    private HangingServer() {
    }

    // The arguments of a main method
    @SuppressWarnings("java:S1172")
    public static void main(String[] args) throws InterruptedException, IOException {
        Files.writeString(Path.of(System.getProperty(PID_FILE)), ProcessHandle.current().pid() + System.lineSeparator(),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        new CountDownLatch(1).await();
    }
}
