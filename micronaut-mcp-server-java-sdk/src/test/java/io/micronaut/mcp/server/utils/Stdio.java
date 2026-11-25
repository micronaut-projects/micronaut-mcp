package io.micronaut.mcp.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Stdio implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Stdio.class);
    public final PipedOutputStream clientToServer;
    public final PipedInputStream serverToClient;   // read responses
    public final PipedInputStream  serverStdin;
    public final PipedOutputStream serverStdout;
    private static final byte NEWLINE = (byte) '\n';
    private BufferedReader lineReader;

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
     * Waits until the server has written {@code count} lines, failing if they do not all arrive within {@code timeout}.
     * Unlike {@link #readResponses()}, it does not depend on sleeping long enough before reading.
     *
     * <p>Call it from the thread that sends the requests and keep it there: {@link PipedInputStream} remembers the last
     * thread on each end of a pipe and reports the pipe as broken once that thread has died, so the I/O must not run on a
     * short-lived thread such as the one {@code assertTimeoutPreemptively} creates. That is why the timeout is enforced by
     * polling here instead.</p>
     *
     * <p>Do not mix it with {@link #readResponses()} on the same instance: each keeps its own buffered reader over the same
     * pipe.</p>
     *
     * @param count   number of lines to read
     * @param timeout how long to wait for all of them
     * @return the lines read
     */
    public synchronized List<String> readLines(int count, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            if (lineReader == null) {
                lineReader = new BufferedReader(new InputStreamReader(serverToClient, StandardCharsets.UTF_8));
            }
            List<String> lines = new ArrayList<>(count);
            while (lines.size() < count) {
                if (lineReader.ready()) {
                    lines.add(lineReader.readLine());
                } else if (System.nanoTime() - deadline > 0) {
                    throw new AssertionError("Expected " + count + " lines within " + timeout + " but read " + lines);
                } else {
                    Thread.sleep(10);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
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
