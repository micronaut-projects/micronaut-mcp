package io.micronaut.mcp.server.stateless.sync;

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

import static io.micronaut.mcp.server.utils.JsonRpcMessages.PING;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PONG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/ping">Ping</a>
 */
@Property(name = "micronaut.mcp.server.type", value = "STATELESS_SYNC")
@MicronautTest
class StatelessSyncPingTest {

    @Test
    void pingSerialization(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", PING);
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        JSONAssert.assertEquals(PONG, responseJson, true);
    }
}
