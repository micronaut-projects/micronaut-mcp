package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.json.JsonMapper;
import io.micronaut.core.type.Argument;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ExecutionModelSupport {
    private ExecutionModelSupport() {
    }

    static Map<String, Object> callTool(HttpClient httpClient, JsonMapper jsonMapper, String name) throws IOException {
        HttpResponse<String> response = httpClient.toBlocking().exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "%s", "arguments": {}}}""".formatted(name)), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        Map<String, Object> body = jsonMapper.readValue(response.body(), Argument.mapOf(String.class, Object.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) body.get("result");
        return result;
    }

    @SuppressWarnings("unchecked")
    static String text(Map<String, Object> result) {
        return ((Map<String, Object>) ((java.util.List<?>) result.get("content")).get(0)).get("text").toString();
    }

    static boolean onEventLoop() {
        return Thread.currentThread().getName().contains("eventLoop");
    }
}
