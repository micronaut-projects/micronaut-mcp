package example.micronaut.mcp;

import io.micronaut.core.io.ResourceLoader;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

import static example.micronaut.ResourceLoaderUtils.readResource;
import static example.micronaut.mcp.JsonRpcMessages.PING;
import static example.micronaut.mcp.JsonRpcMessages.PONG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/ping">Ping</a>
 */
@MicronautTest
class PingTest {

    @Test
    void ping(@Client("/") HttpClient httpClient, ResourceLoader resourceLoader) throws IOException, JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> request = HttpRequest.POST("/mcp", readResource(resourceLoader, "classpath:ping.json").orElseThrow());
        String json = assertDoesNotThrow(() -> client.retrieve(request));
        assertNotNull(json);
        String expectedJson = readResource(resourceLoader, "classpath:pingResponse.json").orElseThrow();
        JSONAssert.assertEquals(expectedJson, json, true);
    }
}
