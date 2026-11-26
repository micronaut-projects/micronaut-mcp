package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class McpControllerTest {

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
            if (method.getName().equals("readValueFromTree")) {
                throw new IOException("unreadable");
            }
            throw new UnsupportedOperationException(method.getName());
        });
        McpController controller = new McpController(
            proxy(McpStatelessServerHandler.class, (proxy, method, args) -> {
                throw new AssertionError("the handler must not be called");
            }),
            request -> null,
            failingJsonMapper,
            proxy(McpJsonMapper.class, (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            }));
        JsonNode body = JsonNode.createObjectNode(Map.of(
            "jsonrpc", JsonNode.createStringNode("2.0"),
            "id", JsonNode.createNumberNode(1),
            "method", JsonNode.createStringNode("tools/call"),
            "params", JsonNode.createObjectNode(Map.of("name", JsonNode.createStringNode("echo")))));

        HttpResponse<?> response = controller.handlePost(HttpRequest.POST("/mcp", ""), body).block();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        Map<?, ?> error = (Map<?, ?>) ((Map<?, ?>) response.body()).get("error");
        assertEquals(McpSchema.ErrorCodes.INVALID_REQUEST, error.get("code"));
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(McpControllerTest.class.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
