package io.micronaut.mcp.client.langchain4j.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An MCP server that answers with crafted responses, written chunk by chunk, to test how a transport handles them. A
 * response is queued for the HTTP method and, for a POST, the JSON-RPC method of the next matching request; without one,
 * a POST is accepted, a GET answered with 405 and a DELETE with 204.
 */
final class CraftedMcpServer implements AutoCloseable {
    static final String SESSION_ID = "Mcp-Session-Id";
    private static final Pattern METHOD = Pattern.compile("\"method\"\\s*:\\s*\"([^\"]+)\"");
    private static final Duration CHUNK_DELAY = Duration.ofMillis(50);

    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private final Map<String, Queue<Responder>> responders = new ConcurrentHashMap<>();

    private CraftedMcpServer(HttpServer server) {
        this.server = server;
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    static CraftedMcpServer start() {
        try {
            return new CraftedMcpServer(HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    int port() {
        return server.getAddress().getPort();
    }

    /**
     * @param key The HTTP method, followed for a POST by the JSON-RPC method, such as {@code POST tools/list}
     * @param responder The response to the next matching request
     */
    void on(String key, Responder responder) {
        responders.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(responder);
    }

    void reset() {
        requests.clear();
        responders.clear();
    }

    List<Request> requests(String method) {
        return requests.stream().filter(r -> r.method().equals(method)).toList();
    }

    /**
     * Waits until the server received the given number of requests of an HTTP method.
     */
    List<Request> await(String method, int count) {
        return await(r -> r.method().equals(method), count);
    }

    List<Request> await(Predicate<Request> filter, int count) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            List<Request> matching = requests.stream().filter(filter).toList();
            if (matching.size() >= count) {
                return matching;
            }
            pause(Duration.ofMillis(20));
        }
        throw new AssertionError("Expected " + count + " matching requests, received " + requests);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) {
        try (exchange) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Request request = new Request(exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getRequestHeaders(), body);
            requests.add(request);
            Matcher method = METHOD.matcher(body);
            String key = request.method().equals("POST") && method.find() ? "POST " + method.group(1) : request.method();
            Queue<Responder> queue = responders.get(key);
            Responder responder = queue != null ? queue.poll() : null;
            if (responder == null) {
                responder = switch (request.method()) {
                    case "POST" -> status(202);
                    case "GET" -> status(405);
                    default -> status(204);
                };
            }
            responder.respond(exchange);
        } catch (IOException _) {
            // The client went away
        }
    }

    static Responder status(int status) {
        return exchange -> exchange.sendResponseHeaders(status, -1);
    }

    /**
     * @param headers Pairs of the names and values of response headers
     */
    static Responder json(int status, String body, String... headers) {
        return body(status, "application/json", body, headers);
    }

    static Responder body(int status, String contentType, String body, String... headers) {
        return exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            for (int i = 0; i < headers.length; i += 2) {
                exchange.getResponseHeaders().add(headers[i], headers[i + 1]);
            }
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
        };
    }

    /**
     * An event stream, written chunk by chunk.
     */
    static Responder events(byte[]... chunks) {
        return exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            OutputStream out = exchange.getResponseBody();
            for (byte[] chunk : chunks) {
                out.write(chunk);
                out.flush();
                pause(CHUNK_DELAY);
            }
        };
    }

    /**
     * A response sent after a delay.
     */
    static Responder delayed(Duration delay, Responder responder) {
        return exchange -> {
            pause(delay);
            responder.respond(exchange);
        };
    }

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static void pause(Duration duration) {
        // Delays the response, as a slow server would
        LockSupport.parkNanos(duration.toNanos());
    }

    @FunctionalInterface
    interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }

    record Request(String method, URI uri, Headers headers, String body) {
        String header(String name) {
            return headers.getFirst(name);
        }
    }
}
