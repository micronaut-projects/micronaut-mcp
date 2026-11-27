package io.micronaut.mcp.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Stdio implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Stdio.class);
    public final PipedOutputStream clientToServer;
    public final PipedInputStream serverToClient;   // read responses
    public final PipedInputStream  serverStdin;
    public final PipedOutputStream serverStdout;
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

    /**
     * Reads one LF-terminated UTF-8 response on the calling thread. Keep pipe I/O on the test thread because
     * {@link PipedInputStream} treats the death of its last reader or writer as a broken pipe.
     *
     * @param timeout how long to wait for the complete response
     * @return the response without its line terminator
     * @throws IOException if the pipe cannot be read
     * @throws InterruptedException if the wait is interrupted
     */
    public String readResponse(Duration timeout) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (System.nanoTime() - deadline < 0) {
            if (serverToClient.available() == 0) {
                TimeUnit.NANOSECONDS.sleep(Math.min(deadline - System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(10)));
                continue;
            }
            // Decode only after the newline. Even a single UTF-8 character can span pipe writes.
            int next = serverToClient.read();
            if (next == NEWLINE) {
                String response = line.toString(StandardCharsets.UTF_8);
                return response.endsWith("\r") ? response.substring(0, response.length() - 1) : response;
            }
            line.write(next);
        }
        throw new AssertionError("Expected a complete stdio response within " + timeout + ", received " + line.size() + " bytes");
    }

    @Override
    public void close() {
        try {
            clientToServer.flush();
            clientToServer.close();

            serverStdout.flush();
            serverStdout.close();

            serverStdin.close();
            serverToClient.close();
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }
}
