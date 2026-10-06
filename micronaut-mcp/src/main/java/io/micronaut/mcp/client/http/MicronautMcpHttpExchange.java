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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.client.StreamingHttpClientRegistry;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientRequestHeaders;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends the JSON-RPC messages of an MCP client to a server over Streamable HTTP with the Micronaut HTTP client, and
 * reads the messages of the response, a JSON body or an event stream, as they arrive.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#streamable-http">Streamable HTTP</a>
 */
@Internal
public final class MicronautMcpHttpExchange {
    private static final String SESSION_ID = "Mcp-Session-Id";
    private static final String PROTOCOL_VERSION = "MCP-Protocol-Version";
    private static final String ACCEPT = MediaType.APPLICATION_JSON + ", " + MediaType.TEXT_EVENT_STREAM;
    private static final Argument<String> ERROR_TYPE = Argument.STRING;

    private final StreamingHttpClient client;
    private final McpClientHttpConfiguration configuration;
    private final List<McpClientHeadersProvider> headersProviders;
    private final boolean dynamicHeaders;
    private final String uri;
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private volatile @Nullable String protocolVersion;

    /**
     * @param registry The registry of the Micronaut streaming HTTP clients
     * @param configuration The connection
     * @param headersProviders The headers providers
     */
    public MicronautMcpHttpExchange(StreamingHttpClientRegistry<?> registry,
                                    McpClientHttpConfiguration configuration,
                                    List<McpClientHeadersProvider> headersProviders) {
        this.configuration = configuration;
        this.headersProviders = headersProviders;
        this.dynamicHeaders = McpClientRequestHeaders.isDynamic(configuration, headersProviders);
        URI url = configuration.getUrl();
        String serviceId = configuration.getServiceId();
        MutableAnnotationMetadata metadata = new MutableAnnotationMetadata();
        if (serviceId != null) {
            // The service configuration names the server, the request carries the path of the endpoint
            metadata.addDeclaredAnnotation(Client.class.getName(), Map.of(AnnotationMetadata.VALUE_MEMBER, serviceId));
            this.uri = url.getRawPath() + (url.getRawQuery() != null ? "?" + url.getRawQuery() : "");
        } else {
            metadata.addDeclaredAnnotation(Client.class.getName(), Map.of(AnnotationMetadata.VALUE_MEMBER, url.getScheme() + "://" + url.getRawAuthority()));
            this.uri = url.toString();
        }
        this.client = registry.getStreamingHttpClient(metadata);
    }

    /**
     * @param protocolVersion The protocol version negotiated with the server, sent with every later request
     */
    public void setProtocolVersion(@Nullable String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /**
     * Sends a JSON-RPC message. The headers of the request are computed on the calling thread.
     *
     * @param message The message, as JSON
     * @return The messages of the response, as JSON: none for an accepted notification, one for a JSON response, or each
     * message of an event stream, as it arrives
     */
    public Flux<String> post(String message) {
        MutableHttpRequest<String> request = HttpRequest.POST(uri, message)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.ACCEPT, ACCEPT);
        Map<String, String> headers = dynamicHeaders
            ? McpClientRequestHeaders.headers(configuration, headersProviders)
            : configuration.getHeaders();
        headers.forEach(request::header);
        String session = sessionId.get();
        if (session != null) {
            request.header(SESSION_ID, session);
        }
        String version = protocolVersion;
        if (version != null) {
            request.header(PROTOCOL_VERSION, version);
        }
        return Flux.defer(() -> {
            ResponseDecoder decoder = new ResponseDecoder();
            return Flux.from(client.exchangeStream(request, ERROR_TYPE))
                .concatMapIterable(decoder::chunk)
                .concatWith(Flux.defer(() -> Flux.fromIterable(decoder.finish())));
        }).onErrorResume(HttpClientResponseException.class, MicronautMcpHttpExchange::errorBody);
    }

    /**
     * Ends the session with the server, if it started one.
     *
     * @return A publisher completing when the session is ended
     */
    public Mono<Void> close() {
        String session = sessionId.getAndSet(null);
        if (session == null) {
            return Mono.empty();
        }
        MutableHttpRequest<?> request = HttpRequest.DELETE(uri).header(SESSION_ID, session);
        return Flux.from(client.exchangeStream(request)).then().onErrorResume(e -> Mono.empty());
    }

    private static Flux<String> errorBody(HttpClientResponseException e) {
        // A JSON-RPC error answered with an error status is still a message for the client
        String body = e.getResponse().getBody(String.class).orElse(null);
        if (body != null && body.trim().startsWith("{") && body.contains("\"jsonrpc\"")) {
            return Flux.just(body);
        }
        return Flux.error(e);
    }

    /**
     * Decodes the body of one response, chunk by chunk.
     */
    private final class ResponseDecoder {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final StringBuilder line = new StringBuilder();
        private final StringBuilder data = new StringBuilder();
        private boolean first = true;
        private boolean eventStream;

        List<String> chunk(HttpResponse<ByteBuffer<?>> response) {
            if (first) {
                first = false;
                response.getHeaders().findFirst(SESSION_ID).ifPresent(sessionId::set);
                eventStream = response.getContentType().map(MediaType.TEXT_EVENT_STREAM_TYPE::matches).orElse(false);
            }
            ByteBuffer<?> buffer = response.body();
            if (buffer == null) {
                return List.of();
            }
            // The client releases the buffer once this chunk is handled
            byte[] bytes = buffer.toByteArray();
            if (!eventStream) {
                body.writeBytes(bytes);
                return List.of();
            }
            return events(new String(bytes, StandardCharsets.UTF_8));
        }

        List<String> finish() {
            if (eventStream) {
                return events("\n\n");
            }
            String json = body.toString(StandardCharsets.UTF_8);
            return json.isBlank() ? List.of() : List.of(json);
        }

        private List<String> events(String text) {
            List<String> messages = new ArrayList<>(1);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    String complete = line.toString();
                    line.setLength(0);
                    int end = complete.endsWith("\r") ? complete.length() - 1 : complete.length();
                    field(complete.substring(0, end), messages);
                } else {
                    line.append(c);
                }
            }
            return messages;
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
            }
        }
    }
}
