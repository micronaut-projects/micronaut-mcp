package io.micronaut.mcp.server.stateless.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest
class McpGetMethodNotAllowedTest {

    @Test
    void pingSerialization(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.GET("/mcp");
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(req, String.class));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getStatus());
    }
}
