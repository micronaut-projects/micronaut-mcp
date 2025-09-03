package io.micronaut.mcp.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
