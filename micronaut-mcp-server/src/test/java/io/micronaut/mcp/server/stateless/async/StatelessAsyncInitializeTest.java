package io.micronaut.mcp.server.stateless.async;

import io.micronaut.context.annotation.Property;
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

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_INITIALIZATION;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle#initialization">Initialization</a>
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.type", value = "STATELESS_ASYNC")
@MicronautTest
class StatelessAsyncInitializeTest {

    @Test
    void initializeSerialization(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", INITIALIZE);
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        JSONAssert.assertEquals(EXPECTED_INITIALIZATION, responseJson, true);
    }
}
