package io.micronaut.mcp.server.stdio;

import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GracefulListingServerTransportProviderTest {

    private RecordingTransport delegateTransport;
    private RecordingProvider delegateProvider;
    private GracefulListingServerTransportProvider provider;
    private McpServerTransport sessionTransport;

    @BeforeEach
    void setUp() {
        delegateTransport = new RecordingTransport();
        delegateProvider = new RecordingProvider(delegateTransport);
        provider = new GracefulListingServerTransportProvider(delegateProvider);
        provider.setSessionFactory(transport -> {
            sessionTransport = transport;
            return null;
        });
    }

    @Test
    void delegateIsRequired() {
        assertThrows(NullPointerException.class, () -> new GracefulListingServerTransportProvider(null));
    }

    static Stream<Arguments> listingMethods() {
        return Stream.of(
            Arguments.of("tools/list", McpSchema.ListToolsResult.class),
            Arguments.of("prompts/list", McpSchema.ListPromptsResult.class),
            Arguments.of("resources/list", McpSchema.ListResourcesResult.class),
            Arguments.of("resources/templates/list", McpSchema.ListResourceTemplatesResult.class)
        );
    }

    @ParameterizedTest
    @MethodSource("listingMethods")
    void methodNotFoundForAListingIsSentAsAnEmptyList(String method, Class<?> resultType) {
        McpSchema.JSONRPCResponse error = McpSchema.JSONRPCResponse.error(7,
            new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND, "Method not found: " + method));

        Mono<Void> result = sessionTransport.sendMessage(error);

        assertSame(delegateTransport.sendResult, result);
        assertEquals(1, delegateTransport.sent.size());
        McpSchema.JSONRPCResponse sent = assertInstanceOf(McpSchema.JSONRPCResponse.class, delegateTransport.sent.get(0));
        assertEquals(7, sent.id());
        assertNull(sent.error());
        Object listResult = assertInstanceOf(resultType, sent.result());
        assertTrue(emptyItems(listResult));
    }

    static Stream<McpSchema.JSONRPCMessage> messagesSentUnchanged() {
        return Stream.of(
            McpSchema.JSONRPCResponse.error(1,
                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND, "Method not found: foo/bar")),
            McpSchema.JSONRPCResponse.result(2, Map.of("tools", List.of())),
            new McpSchema.JSONRPCRequest("ping", 3),
            new McpSchema.JSONRPCNotification("notifications/tools/list_changed")
        );
    }

    @ParameterizedTest
    @MethodSource("messagesSentUnchanged")
    void otherMessagesReachTheDelegateUnchanged(McpSchema.JSONRPCMessage message) {
        Mono<Void> result = sessionTransport.sendMessage(message);

        assertSame(delegateTransport.sendResult, result);
        assertEquals(1, delegateTransport.sent.size());
        assertSame(message, delegateTransport.sent.get(0));
    }

    @Test
    void transportUnmarshalFromDelegates() {
        Object data = new Object();
        TypeRef<String> typeRef = new TypeRef<>() { };

        assertSame(RecordingTransport.UNMARSHALLED, sessionTransport.unmarshalFrom(data, typeRef));
        assertSame(data, delegateTransport.unmarshalData);
        assertSame(typeRef, delegateTransport.unmarshalTypeRef);
    }

    @Test
    void transportCloseGracefullyDelegates() {
        assertSame(delegateTransport.closeGracefullyResult, sessionTransport.closeGracefully());
    }

    @Test
    void transportCloseDelegates() {
        sessionTransport.close();
        assertEquals(1, delegateTransport.closeCount);
    }

    @Test
    void transportProtocolVersionsDelegates() {
        assertSame(delegateTransport.protocolVersions, sessionTransport.protocolVersions());
    }

    @Test
    void providerNotifyClientsDelegates() {
        Object params = new Object();

        assertSame(delegateProvider.notifyClientsResult, provider.notifyClients("notifications/message", params));
        assertEquals(List.of("notifications/message"), delegateProvider.notifiedMethods);
        assertSame(params, delegateProvider.notifiedParams);
    }

    @Test
    void providerNotifyClientDelegates() {
        Object params = new Object();

        assertSame(delegateProvider.notifyClientResult, provider.notifyClient("session-1", "notifications/message", params));
        assertEquals("session-1", delegateProvider.notifiedSessionId);
        assertEquals(List.of("notifications/message"), delegateProvider.notifiedMethods);
        assertSame(params, delegateProvider.notifiedParams);
    }

    @Test
    void providerCloseGracefullyDelegates() {
        assertSame(delegateProvider.closeGracefullyResult, provider.closeGracefully());
    }

    @Test
    void providerCloseDelegates() {
        provider.close();
        assertEquals(1, delegateProvider.closeCount);
    }

    @Test
    void providerProtocolVersionsDelegates() {
        assertSame(delegateProvider.protocolVersions, provider.protocolVersions());
    }

    private static boolean emptyItems(Object listResult) {
        if (listResult instanceof McpSchema.ListToolsResult r) {
            return r.tools().isEmpty();
        }
        if (listResult instanceof McpSchema.ListPromptsResult r) {
            return r.prompts().isEmpty();
        }
        if (listResult instanceof McpSchema.ListResourcesResult r) {
            return r.resources().isEmpty();
        }
        if (listResult instanceof McpSchema.ListResourceTemplatesResult r) {
            return r.resourceTemplates().isEmpty();
        }
        return false;
    }

    private static Mono<Void> distinctMono() {
        return Mono.fromRunnable(() -> { });
    }

    private static final class RecordingTransport implements McpServerTransport {
        static final Object UNMARSHALLED = new Object();

        final List<McpSchema.JSONRPCMessage> sent = new ArrayList<>();
        final Mono<Void> sendResult = distinctMono();
        final Mono<Void> closeGracefullyResult = distinctMono();
        final List<String> protocolVersions = List.of("test-version");
        Object unmarshalData;
        TypeRef<?> unmarshalTypeRef;
        int closeCount;

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            sent.add(message);
            return sendResult;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            unmarshalData = data;
            unmarshalTypeRef = typeRef;
            return (T) UNMARSHALLED;
        }

        @Override
        public Mono<Void> closeGracefully() {
            return closeGracefullyResult;
        }

        @Override
        public void close() {
            closeCount++;
        }

        @Override
        public List<String> protocolVersions() {
            return protocolVersions;
        }
    }

    private static final class RecordingProvider implements McpServerTransportProvider {
        final RecordingTransport transport;
        final Mono<Void> notifyClientsResult = distinctMono();
        final Mono<Void> notifyClientResult = distinctMono();
        final Mono<Void> closeGracefullyResult = distinctMono();
        final List<String> protocolVersions = List.of("test-version");
        final List<String> notifiedMethods = new ArrayList<>();
        Object notifiedParams;
        String notifiedSessionId;
        int closeCount;

        RecordingProvider(RecordingTransport transport) {
            this.transport = transport;
        }

        @Override
        public void setSessionFactory(McpServerSession.Factory sessionFactory) {
            sessionFactory.create(transport);
        }

        @Override
        public Mono<Void> notifyClients(String method, Object params) {
            notifiedMethods.add(method);
            notifiedParams = params;
            return notifyClientsResult;
        }

        @Override
        public Mono<Void> notifyClient(String sessionId, String method, Object params) {
            notifiedSessionId = sessionId;
            notifiedMethods.add(method);
            notifiedParams = params;
            return notifyClientResult;
        }

        @Override
        public Mono<Void> closeGracefully() {
            return closeGracefullyResult;
        }

        @Override
        public void close() {
            closeCount++;
        }

        @Override
        public List<String> protocolVersions() {
            return protocolVersions;
        }
    }
}
