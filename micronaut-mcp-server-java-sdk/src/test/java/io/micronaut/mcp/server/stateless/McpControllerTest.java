package io.micronaut.mcp.server.stateless;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.JsonStreamConfig;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.mcp.server.json.MicronautMcpJsonMapperSupplier;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpControllerTest {
    private static final String SECRET = "secret detail";

    private final Logger logger = (Logger) LoggerFactory.getLogger(McpController.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(logs);
        logs.stop();
    }

    static Stream<Arguments> errorCodesAndExpectedStatuses() {
        return Stream.of(
            Arguments.of(McpSchema.ErrorCodes.PARSE_ERROR, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.INVALID_REQUEST, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.METHOD_NOT_FOUND, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.INVALID_PARAMS, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
            // Unknown/unspecified error codes should default to 500
            Arguments.of(42, HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    @ParameterizedTest
    @MethodSource("errorCodesAndExpectedStatuses")
    void statusMapsJsonRpcErrorCodeToHttpStatus(int errorCode, HttpStatus expected) {
        McpSchema.JSONRPCResponse.JSONRPCError jsonRpcError = McpError
            .builder(errorCode)
            .message("test")
            .build()
            .getJsonRpcError();

        HttpStatus actual = McpController.status(jsonRpcError);
        assertEquals(expected, actual);
    }

    @Test
    void parametersTheJsonMapperCannotReadAreAnInvalidRequest() {
        // An application McpJsonMapper gets the parameters as plain objects, which an application JsonMapper may fail to read
        JsonMapper failingJsonMapper = proxy(JsonMapper.class, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getStreamConfig" -> JsonStreamConfig.DEFAULT;
                case "readValueFromTree" -> throw new IOException("unreadable");
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
        McpController controller = new McpReactiveController(
            proxy(McpStatelessServerHandler.class, (proxy, method, args) -> {
                throw new AssertionError("the handler must not be called");
            }),
            request -> null,
            failingJsonMapper,
            proxy(McpJsonMapper.class, (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            }),
            McpSchema.ServerCapabilities.builder().build(),
            // handle is given whether the response may stream, so the policy is not used
            null);
        JsonNode body = JsonNode.createObjectNode(Map.of(
            "jsonrpc", JsonNode.createStringNode("2.0"),
            "id", JsonNode.createNumberNode(1),
            "method", JsonNode.createStringNode("tools/call"),
            "params", JsonNode.createObjectNode(Map.of("name", JsonNode.createStringNode("echo")))));

        HttpResponse<?> response = controller.handle(HttpRequest.POST("/mcp", ""), body, false).block();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        JsonNode error = ((JsonNode) response.body()).get("error");
        assertEquals(McpSchema.ErrorCodes.INVALID_REQUEST, error.get("code").getIntValue());
        assertEquals("Invalid Request: \"params\" could not be read", error.get("message").getStringValue());
    }

    static Stream<Arguments> idsAndFailingHandlers() {
        Named<Function<McpSchema.JSONRPCRequest, Mono<McpSchema.JSONRPCResponse>>> throwing = Named.of("throws", request -> {
            throw new IllegalStateException(SECRET);
        });
        Named<Function<McpSchema.JSONRPCRequest, Mono<McpSchema.JSONRPCResponse>>> failing =
            Named.of("emits an error", request -> Mono.error(new IllegalStateException(SECRET)));
        return Stream.of(
            Arguments.of(Named.of("5", JsonNode.createNumberNode(5)), 5, throwing),
            Arguments.of(Named.of("call-5", JsonNode.createStringNode("call-5")), "call-5", throwing),
            Arguments.of(Named.of("12345678901", JsonNode.createNumberNode(12345678901L)), 12345678901L, throwing),
            Arguments.of(Named.of("5", JsonNode.createNumberNode(5)), 5, failing),
            Arguments.of(Named.of("call-5", JsonNode.createStringNode("call-5")), "call-5", failing));
    }

    @ParameterizedTest
    @MethodSource("idsAndFailingHandlers")
    void aRequestHandlerFailureIsAnInternalErrorThatIsLoggedButNotShown(JsonNode id, Object expectedId,
            Function<McpSchema.JSONRPCRequest, Mono<McpSchema.JSONRPCResponse>> handler) {
        McpController controller = controller(new StubHandler(handler, Mono::empty));

        HttpResponse<?> response = controller.handle(HttpRequest.POST("/mcp", ""), request(id), false).block();

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        McpSchema.JSONRPCResponse rsp = (McpSchema.JSONRPCResponse) response.body();
        assertEquals(expectedId, rsp.id());
        assertEquals(McpSchema.ErrorCodes.INTERNAL_ERROR, rsp.error().code());
        assertEquals("Internal error", rsp.error().message());
        assertFalse(rsp.toString().contains(SECRET));
        ILoggingEvent event = singleError();
        assertTrue(event.getFormattedMessage().contains("tools/call"), event.getFormattedMessage());
        assertNotNull(event.getThrowableProxy());
        assertEquals(SECRET, event.getThrowableProxy().getMessage());
    }

    @Test
    void anMcpErrorKeepsItsCodeAndMessageAndIsNotLoggedAsAnError() {
        McpController controller = controller(new StubHandler(
            request -> Mono.error(McpError.builder(McpSchema.ErrorCodes.METHOD_NOT_FOUND).message("Unknown tool").build()),
            Mono::empty));

        HttpResponse<?> response = controller.handle(HttpRequest.POST("/mcp", ""), request(JsonNode.createNumberNode(5)), false).block();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        McpSchema.JSONRPCResponse rsp = (McpSchema.JSONRPCResponse) response.body();
        assertEquals(5, rsp.id());
        assertEquals(McpSchema.ErrorCodes.METHOD_NOT_FOUND, rsp.error().code());
        assertEquals("Unknown tool", rsp.error().message());
        assertTrue(logs.list.stream().noneMatch(e -> e.getLevel() == Level.ERROR));
    }

    static Stream<Arguments> failingNotificationHandlers() {
        Supplier<Mono<Void>> throwing = () -> {
            throw new IllegalStateException(SECRET);
        };
        Supplier<Mono<Void>> failing = () -> Mono.error(new IllegalStateException(SECRET));
        return Stream.of(Arguments.of(Named.of("throws", throwing)), Arguments.of(Named.of("emits an error", failing)));
    }

    @ParameterizedTest
    @MethodSource("failingNotificationHandlers")
    void aNotificationHandlerFailureIsAServerErrorThatIsLoggedOnce(Supplier<Mono<Void>> handler) {
        McpController controller = controller(new StubHandler(request -> Mono.empty(), handler));

        HttpResponse<?> response = controller.handle(HttpRequest.POST("/mcp", ""), notification(), false).block();

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(response.getBody().isEmpty());
        ILoggingEvent event = singleError();
        assertTrue(event.getFormattedMessage().contains("notifications/initialized"), event.getFormattedMessage());
        assertEquals(SECRET, event.getThrowableProxy().getMessage());
    }

    @Test
    void aNotificationMcpErrorGivesItsStatusAndIsNotLoggedAsAnError() {
        McpController controller = controller(new StubHandler(request -> Mono.empty(),
            () -> Mono.error(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("Bad params").build())));

        HttpResponse<?> response = controller.handle(HttpRequest.POST("/mcp", ""), notification(), false).block();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().isEmpty());
        assertTrue(logs.list.stream().noneMatch(e -> e.getLevel() == Level.ERROR));
    }

    @ParameterizedTest
    @MethodSource("methodsAndLoggedNames")
    void onlyAShortPrintableMethodNameIsLogged(String method, String loggedName) {
        McpController controller = controller(new StubHandler(request -> {
            throw new IllegalStateException(SECRET);
        }, Mono::empty));

        controller.handle(HttpRequest.POST("/mcp", ""), request(JsonNode.createNumberNode(5), method), false).block();

        assertEquals("Failed to handle JSON-RPC request " + loggedName, singleError().getFormattedMessage());
    }

    static Stream<Arguments> methodsAndLoggedNames() {
        return Stream.of(
            Arguments.of("tools/call", "tools/call"),
            Arguments.of("tools/call\n12:00:00 INFO forged line", "<non-printable method>"),
            Arguments.of("tools/call\u001b[31m", "<non-printable method>"),
            Arguments.of("t".repeat(100), "t".repeat(64)));
    }

    private ILoggingEvent singleError() {
        List<ILoggingEvent> errors = logs.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(1, errors.size(), errors::toString);
        return errors.get(0);
    }

    private static JsonNode notification() {
        return JsonNode.createObjectNode(Map.of(
            "jsonrpc", JsonNode.createStringNode("2.0"),
            "method", JsonNode.createStringNode("notifications/initialized")));
    }

    private static JsonNode request(JsonNode id) {
        return request(id, "tools/call");
    }

    private static JsonNode request(JsonNode id, String method) {
        return JsonNode.createObjectNode(Map.of(
            "jsonrpc", JsonNode.createStringNode("2.0"),
            "id", id,
            "method", JsonNode.createStringNode(method),
            "params", JsonNode.createObjectNode(Map.of("name", JsonNode.createStringNode("echo")))));
    }

    private static McpController controller(McpStatelessServerHandler handler) {
        return new McpReactiveController(handler,
            request -> McpTransportContext.EMPTY,
            proxy(JsonMapper.class, (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            }),
            new MicronautMcpJsonMapperSupplier().get(),
            McpSchema.ServerCapabilities.builder().build(),
            // handle is given whether the response may stream, so the policy is not used
            null);
    }

    private record StubHandler(Function<McpSchema.JSONRPCRequest, Mono<McpSchema.JSONRPCResponse>> onRequest,
                               Supplier<Mono<Void>> onNotification) implements McpStatelessServerHandler {
        @Override
        public Mono<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext transportContext, McpSchema.JSONRPCRequest request) {
            return onRequest.apply(request);
        }

        @Override
        public Mono<Void> handleNotification(McpTransportContext transportContext, McpSchema.JSONRPCNotification notification) {
            return onNotification.get();
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(McpControllerTest.class.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
