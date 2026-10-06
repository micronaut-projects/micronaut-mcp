package io.micronaut.mcp.server.context;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpLoggableSession;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMcpRequestContextTest {
    private static final McpSchema.CallToolRequest REQUEST = McpSchema.CallToolRequest.builder()
        .name("tool")
        .arguments(Map.of())
        .progressToken("token")
        .build();
    private static final McpSchema.ElicitRequest ELICIT = McpSchema.ElicitRequest.builder()
        .message("Name?")
        .requestedSchema(Map.of("type", "object"))
        .build();
    private static final McpSchema.CreateMessageRequest SAMPLE = McpSchema.CreateMessageRequest.builder()
        .messages(List.of())
        .maxTokens(10)
        .build();

    @Test
    void theSyncExchangeSendsNotificationsAndRequests() {
        RecordingSession session = new RecordingSession();
        McpSyncServerExchange exchange = new McpSyncServerExchange(exchange(session));
        McpRequestContext context = DefaultMcpRequestContext.of(exchange, REQUEST);
        assertNotificationsAndRequests(context, session);
        assertSame(exchange.transportContext(), context.transportContext());
    }

    @Test
    void theAsyncExchangeSendsNotificationsAndRequests() {
        RecordingSession session = new RecordingSession();
        McpRequestContext context = DefaultMcpRequestContext.of(exchange(session), REQUEST);
        assertNotificationsAndRequests(context, session);
    }

    @Test
    void theEmitterOfTheHttpRequestSendsNotifications() {
        RecordingEmitter emitter = new RecordingEmitter();
        MutableHttpRequest<?> request = HttpRequest.POST("/mcp", "");
        request.setAttribute(McpNotificationEmitter.ATTRIBUTE, emitter);
        McpRequestContext context = DefaultMcpRequestContext.of(
            new HttpRequestMcpTransportContext(request, r -> null, new NoLocale()), REQUEST);
        context.progress(1);
        context.log(McpSchema.LoggingLevel.INFO, "message");
        assertEquals(List.of(McpSchema.METHOD_NOTIFICATION_PROGRESS, McpSchema.METHOD_NOTIFICATION_MESSAGE),
            emitter.notifications.stream().map(McpSchema.JSONRPCNotification::method).toList());
        assertFalse(context.isCancelled());
        emitter.cancelled = true;
        assertTrue(context.isCancelled());
    }

    @Test
    void withoutAnExchangeOrAnEmitterNotificationsAreDroppedAndRequestsUnsupported() {
        McpRequestContext context = DefaultMcpRequestContext.of(null, null);
        assertSame(McpTransportContext.EMPTY, context.transportContext());
        assertNull(context.progressToken());
        context.progress(1);
        context.log(McpSchema.LoggingLevel.INFO, "message");
        assertFalse(context.isCancelled());
        assertThrows(UnsupportedOperationException.class, () -> context.elicit(ELICIT));
        assertThrows(UnsupportedOperationException.class, () -> context.sample(SAMPLE));
        McpRequestContext withToken = DefaultMcpRequestContext.of(McpTransportContext.EMPTY, REQUEST);
        assertEquals("token", withToken.progressToken());
        withToken.progress(1);
    }

    private static void assertNotificationsAndRequests(McpRequestContext context, RecordingSession session) {
        assertEquals("token", context.progressToken());
        context.progress(1);
        context.log(McpSchema.LoggingLevel.INFO, "message");
        assertEquals(List.of(McpSchema.METHOD_NOTIFICATION_PROGRESS, McpSchema.METHOD_NOTIFICATION_MESSAGE), session.notifications);
        assertEquals(McpSchema.ElicitResult.Action.ACCEPT, context.elicit(ELICIT).action());
        assertEquals("sampled", ((McpSchema.TextContent) context.sample(SAMPLE).content()).text());
        assertFalse(context.isCancelled());
    }

    private static McpAsyncServerExchange exchange(RecordingSession session) {
        McpSchema.ClientCapabilities capabilities = McpSchema.ClientCapabilities.builder().elicitation().sampling().build();
        return new McpAsyncServerExchange("session", session, capabilities, new McpSchema.Implementation("client", "1.0"),
            McpTransportContext.create(Map.of()));
    }

    private static final class RecordingSession implements McpLoggableSession {
        private final List<String> notifications = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> Mono<T> sendRequest(String method, Object requestParams, TypeRef<T> typeRef) {
            Object result = McpSchema.METHOD_ELICITATION_CREATE.equals(method)
                ? new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, Map.of())
                : McpSchema.CreateMessageResult.builder().content(new McpSchema.TextContent("sampled")).model("model").build();
            return Mono.just((T) result);
        }

        @Override
        public Mono<Void> sendNotification(String method, Object params) {
            notifications.add(method);
            return Mono.empty();
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.empty();
        }

        @Override
        public void close() {
            // nothing to release
        }

        @Override
        public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
            // every level is sent
        }

        @Override
        public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
            return true;
        }
    }

    private static final class RecordingEmitter implements McpNotificationEmitter {
        private final List<McpSchema.JSONRPCNotification> notifications = new ArrayList<>();
        private boolean cancelled;

        @Override
        public void emit(McpSchema.JSONRPCNotification notification) {
            notifications.add(notification);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    private static final class NoLocale implements io.micronaut.core.util.LocaleResolver<HttpRequest<?>> {
        @Override
        public Optional<Locale> resolve(HttpRequest<?> request) {
            return Optional.empty();
        }

        @Override
        public Locale resolveOrDefault(HttpRequest<?> request) {
            return Locale.ENGLISH;
        }
    }
}
