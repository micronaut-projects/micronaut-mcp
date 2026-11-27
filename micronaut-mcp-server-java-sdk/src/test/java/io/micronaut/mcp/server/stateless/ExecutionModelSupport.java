package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.json.JsonMapper;
import io.micronaut.core.type.Argument;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class ExecutionModelSupport {
    private ExecutionModelSupport() {
    }

    static Map<String, Object> callTool(HttpClient httpClient, JsonMapper jsonMapper, String name) throws IOException {
        return callTool(httpClient, jsonMapper, name, "{}");
    }

    /**
     * @param arguments The arguments of the call, as a JSON object
     * @return The result of a tool call that succeeds or fails with a tool execution error
     */
    static Map<String, Object> callTool(HttpClient httpClient, JsonMapper jsonMapper, String name, String arguments) throws IOException {
        HttpResponse<String> response = httpClient.toBlocking().exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "%s", "arguments": %s}}""".formatted(name, arguments)), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        return result(jsonMapper, response.body());
    }

    /**
     * @return The JSON-RPC error of a tool call that fails with a protocol error
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> callToolError(HttpClient httpClient, JsonMapper jsonMapper, String name, HttpStatus status) throws IOException {
        HttpRequest<String> request = HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "%s", "arguments": {}}}""".formatted(name));
        String body;
        try {
            body = httpClient.toBlocking().retrieve(request);
        } catch (HttpClientResponseException e) {
            assertEquals(status, e.getStatus());
            body = e.getResponse().getBody(String.class).orElseThrow();
        }
        Map<String, Object> response = jsonMapper.readValue(body, Argument.mapOf(String.class, Object.class));
        // a protocol error has no result, so it cannot also be a tool execution error
        assertFalse(response.containsKey("result"), body);
        return (Map<String, Object>) response.get("error");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> complete(HttpClient httpClient, JsonMapper jsonMapper, String prompt) throws IOException {
        String body = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "completion/complete", "params": {"ref": {"type": "ref/prompt", "name": "%s"}, "argument": {"name": "name", "value": "A"}}}""".formatted(prompt)));
        return (Map<String, Object>) result(jsonMapper, body).get("completion");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> result(JsonMapper jsonMapper, String body) throws IOException {
        Map<String, Object> response = jsonMapper.readValue(body, Argument.mapOf(String.class, Object.class));
        return (Map<String, Object>) response.get("result");
    }

    @SuppressWarnings("unchecked")
    static String text(Map<String, Object> result) {
        return ((Map<String, Object>) ((List<?>) result.get("content")).get(0)).get("text").toString();
    }

    static boolean onEventLoop() {
        return Thread.currentThread().getName().contains("eventLoop");
    }
}
