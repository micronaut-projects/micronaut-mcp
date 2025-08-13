package io.micronaut.mcp.server.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Stdio implements AutoCloseable {
    public final PipedOutputStream clientToServer;
    public final PipedInputStream serverToClient;   // read responses
    public final PipedInputStream  serverStdin;
    public final PipedOutputStream serverStdout;
    private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
    private static final byte NEWLINE = (byte) '\n';

    public Stdio() {
        try {
            this.clientToServer = new PipedOutputStream();
            this.serverStdin = new PipedInputStream(clientToServer, 64 * 1024);

            this.serverStdout = new PipedOutputStream();
            this.serverToClient = new PipedInputStream(serverStdout, 64 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendRequest(String json) throws IOException {
        clientToServer.write(json.getBytes(StandardCharsets.UTF_8));
        clientToServer.write(NEWLINE);
        clientToServer.flush();
    }

    public List<String> readResponses() {
        try {
            List<String> lines = new ArrayList<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(serverToClient, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
                if (!br.ready()) {
                    break;
                }
            }
            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try { clientToServer.flush(); } catch (IOException ignored) {}
        try { clientToServer.close(); } catch (IOException ignored) {}
        try { serverStdout.flush(); } catch (IOException ignored) {}
        try { serverStdout.close(); } catch (IOException ignored) {}

        serverExecutor.shutdown();
        try {
            serverExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (!serverExecutor.isTerminated()) {
                serverExecutor.shutdownNow();
            }
        }

        try { serverStdin.close(); } catch (IOException ignored) {}
        try { serverToClient.close(); } catch (IOException ignored) {}
    }
}
