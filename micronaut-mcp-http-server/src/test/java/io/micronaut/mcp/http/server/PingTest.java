package io.micronaut.mcp.http.server;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.jsonrpc.Request;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/ping">Ping</a>
 */
@MicronautTest
class PingTest {

    @Test
    void pingSerialization(@Client("/") HttpClient httpClient,
                           JsonMapper jsonMapper) throws IOException {
        BlockingHttpClient client = httpClient.toBlocking();
        var request = new Request<>("ping", "123");
        String json = jsonMapper.writeValueAsString(request);
        assertEquals("""
            {"jsonrpc":"2.0","method":"ping","id":"123"}""", json);
        HttpRequest<?> req = HttpRequest.POST("/mcp", json);
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        assertEquals("""
            {"jsonrpc":"2.0","result":{},"id":"123"}""", responseJson);
    }
}
