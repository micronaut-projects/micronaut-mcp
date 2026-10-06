/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mcp.client.http;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientRequestHeaders;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends the JSON-RPC messages of an MCP client to a server over Streamable HTTP with the Micronaut HTTP client, and
 * reads the messages of the response, a JSON body or an event stream, as they arrive. The messages are decoded on the
 * event loop, then published on a scheduler that may block, which parses them and calls the client.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#streamable-http">Streamable HTTP</a>
 */
@Internal
public final class MicronautMcpHttpExchange {
    /**
     * The header of the session id.
     */
    public static final String SESSION_ID = "Mcp-Session-Id";
    /**
     * The header of the negotiated protocol version.
     */
    public static final String PROTOCOL_VERSION = "MCP-Protocol-Version";
    private static final String ACCEPT = MediaType.APPLICATION_JSON + ", " + MediaType.TEXT_EVENT_STREAM;
    private static final Argument<String> ERROR_TYPE = Argument.STRING;
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5);

    private final StreamingHttpClient client;
    private final String uri;
    private final McpClientHttpConfiguration configuration;
    private final List<McpClientHeadersProvider> headersProviders;
    private final boolean dynamicHeaders;
    private final Scheduler scheduler;
    private final long maxMessageSize;
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<String> protocolVersion = new AtomicReference<>();
    private final AtomicBoolean sessionless = new AtomicBoolean();

    MicronautMcpHttpExchange(StreamingHttpClient client,
                             String uri,
                             McpClientHttpConfiguration configuration,
                             List<McpClientHeadersProvider> headersProviders,
                             Scheduler scheduler) {
        this.client = client;
        this.uri = uri;
        this.configuration = configuration;
        this.headersProviders = headersProviders;
        this.dynamicHeaders = McpClientRequestHeaders.isDynamic(configuration, headersProviders);
        this.scheduler = scheduler;
        this.maxMessageSize = configuration.getMaxMessageSize();
    }

    /**
     * @param protocolVersion The protocol version negotiated with the server, sent with every later request
     */
    public void setProtocolVersion(@Nullable String protocolVersion) {
        this.protocolVersion.set(protocolVersion);
    }

    /**
     * @param sessionless Whether the protocol has no sessions, as the 2026-07-28 protocol, so that no session id is sent
     */
    public void setSessionless(boolean sessionless) {
        this.sessionless.set(sessionless);
        if (sessionless) {
            sessionId.set(null);
        }
    }

    /**
     * Computes the headers of a request on the calling thread: the static headers of the connection, and those of the
     * headers providers.
     *
     * @return The headers
     */
    public Map<String, String> headers() {
        return dynamicHeaders ? McpClientRequestHeaders.headers(configuration, headersProviders) : configuration.getHeaders();
    }

    /**
     * Sends a JSON-RPC message.
     *
     * @param message The message, as JSON
     * @param headers The headers of the request, such as those {@link #headers() computed} when the client was called
     * @param initialize Whether the message initializes a session, which is then sent without the previous session id
     * @return The messages of the response, as JSON: none for an accepted notification, one for a JSON response, or each
     * message of an event stream, as it arrives. A JSON-RPC error answered with an error status is a message too. Fails
     * with {@link McpHttpSessionNotFoundException} when the server no longer knows the session.
     */
    public Flux<String> post(String message, Map<String, String> headers, boolean initialize) {
        MutableHttpRequest<String> request = HttpRequest.POST(uri, message)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.ACCEPT, ACCEPT);
        headers.forEach(request::header);
        String session = initialize ? null : sessionHeaders(request);
        if (initialize) {
            sessionId.set(null);
        }
        return exchange(request, session, true);
    }

    /**
     * Opens the stream of the messages the server sends on its own, such as requests and notifications.
     *
     * @param headers The headers of the request
     * @return The messages of the stream. Fails with {@link McpHttpSessionNotFoundException} when the server no longer knows
     * the session, and with an {@link HttpClientResponseException} of status 405 when the server does not offer a stream.
     */
    public Flux<String> listen(Map<String, String> headers) {
        MutableHttpRequest<?> request = HttpRequest.GET(uri).header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM);
        headers.forEach(request::header);
        String session = sessionHeaders(request);
        return exchange(request, session, false);
    }

    private @Nullable String sessionHeaders(MutableHttpRequest<?> request) {
        String version = protocolVersion.get();
        if (version != null) {
            request.header(PROTOCOL_VERSION, version);
        }
        String session = sessionId.get();
        if (session != null && !sessionless.get()) {
            request.header(SESSION_ID, session);
            return session;
        }
        return null;
    }

    private Flux<String> exchange(MutableHttpRequest<?> request, @Nullable String session, boolean errorMessages) {
        return Flux.defer(() -> {
            ResponseDecoder decoder = new ResponseDecoder(splitsLines(request));
            return Flux.from(client.exchangeStream(request, ERROR_TYPE))
                .concatMapIterable(decoder::chunk)
                .concatWith(Flux.defer(() -> Flux.fromIterable(decoder.finish())));
        }).onErrorResume(HttpClientResponseException.class, e -> errorBody(e, session, errorMessages))
            .publishOn(scheduler);
    }

    private static boolean splitsLines(MutableHttpRequest<?> request) {
        // As the Micronaut HTTP client, which splits the event stream of a request that only accepts one into lines
        return MediaType.TEXT_EVENT_STREAM.equalsIgnoreCase(request.getHeaders().get(HttpHeaders.ACCEPT));
    }

    /**
     * Ends the session with the server, if it started one, waiting a bounded time for the server to answer.
     *
     * @param headers The headers of the request
     * @return A publisher completing when the session is ended
     */
    public Mono<Void> close(Map<String, String> headers) {
        MutableHttpRequest<?> request = HttpRequest.DELETE(uri);
        headers.forEach(request::header);
        String session = sessionHeaders(request);
        sessionId.set(null);
        if (session == null) {
            return Mono.empty();
        }
        return Flux.from(client.exchangeStream(request)).then()
            .timeout(CLOSE_TIMEOUT)
            .onErrorResume(e -> Mono.empty());
    }

    private Flux<String> errorBody(HttpClientResponseException e, @Nullable String session, boolean errorMessages) {
        if (e.getStatus() == HttpStatus.NOT_FOUND && session != null) {
            // The server no longer knows the session: forget it, so that the client initializes a new one
            sessionId.compareAndSet(session, null);
            return Flux.error(new McpHttpSessionNotFoundException(session, "The MCP server " + configuration.getName() + " no longer knows the session " + session, e));
        }
        if (!errorMessages) {
            return Flux.error(e);
        }
        // A JSON-RPC error answered with an error status is still a message for the client
        String body = e.getResponse().getBody(String.class).orElse(null);
        if (body != null && body.length() <= maxMessageSize && body.trim().startsWith("{") && body.contains("\"jsonrpc\"")) {
            return Flux.just(body);
        }
        return Flux.error(e);
    }

    private IllegalStateException tooLarge() {
        return new IllegalStateException("A message of the MCP server " + configuration.getName() + " exceeds the maximum size of "
            + maxMessageSize + " bytes, set by max-message-size");
    }

    /**
     * Decodes the body of one response, chunk by chunk. The lines of an event stream are split on their bytes, and each
     * is decoded once complete, so that a character split across chunks is decoded whole. When the request only accepts
     * an event stream, the Micronaut HTTP client splits the stream into lines itself, without their line ends, and each
     * chunk is a line.
     */
    private final class ResponseDecoder {
        private final boolean splitLines;
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();
        private final StringBuilder data = new StringBuilder();
        private boolean first = true;
        private boolean eventStream;

        ResponseDecoder(boolean splitLines) {
            this.splitLines = splitLines;
        }

        List<String> chunk(HttpResponse<ByteBuffer<?>> response) {
            if (first) {
                first = false;
                if (!sessionless.get()) {
                    response.getHeaders().findFirst(SESSION_ID).ifPresent(sessionId::set);
                }
                eventStream = response.getContentType().map(MediaType.TEXT_EVENT_STREAM_TYPE::matches).orElse(false);
            }
            ByteBuffer<?> buffer = response.body();
            if (buffer == null) {
                return List.of();
            }
            // The client releases the buffer once this chunk is handled
            byte[] bytes = buffer.toByteArray();
            if (!eventStream) {
                if (body.size() + (long) bytes.length > maxMessageSize) {
                    throw tooLarge();
                }
                body.writeBytes(bytes);
                return List.of();
            }
            if (splitLines) {
                List<String> messages = new ArrayList<>(1);
                field(decodeLine(bytes, 0, bytes.length), messages);
                return messages;
            }
            return events(bytes);
        }

        List<String> finish() {
            if (eventStream) {
                List<String> messages = new ArrayList<>(1);
                if (line.size() > 0) {
                    field(decodeLine(line.toByteArray(), 0, line.size()), messages);
                    line.reset();
                }
                field("", messages);
                return messages;
            }
            String json = body.toString(StandardCharsets.UTF_8);
            return json.isBlank() ? List.of() : List.of(json);
        }

        private List<String> events(byte[] bytes) {
            List<String> messages = new ArrayList<>(1);
            int start = 0;
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] == '\n') {
                    String text;
                    if (line.size() == 0) {
                        text = decodeLine(bytes, start, i);
                    } else {
                        line.write(bytes, start, i - start);
                        text = decodeLine(line.toByteArray(), 0, line.size());
                        line.reset();
                    }
                    field(text, messages);
                    start = i + 1;
                }
            }
            if (start < bytes.length) {
                if (line.size() + (long) (bytes.length - start) > maxMessageSize) {
                    throw tooLarge();
                }
                line.write(bytes, start, bytes.length - start);
            }
            return messages;
        }

        private static String decodeLine(byte[] bytes, int start, int end) {
            int last = end > start && bytes[end - 1] == '\r' ? end - 1 : end;
            return new String(bytes, start, last - start, StandardCharsets.UTF_8);
        }

        private void field(String field, List<String> messages) {
            if (field.isEmpty()) {
                // A blank line dispatches the event
                if (!data.isEmpty()) {
                    messages.add(data.toString());
                    data.setLength(0);
                }
            } else if (field.startsWith("data:")) {
                if (!data.isEmpty()) {
                    data.append('\n');
                }
                data.append(field, field.startsWith("data: ") ? 6 : 5, field.length());
                if (data.length() > maxMessageSize) {
                    throw tooLarge();
                }
            }
        }
    }
}
