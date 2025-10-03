package io.micronaut.mcp.server.stateless.sync.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.info.name", value="test mcp server")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest
class ResourcesListNoResourcesCapabilityTest {
    public static final String RESOURCES_LIST = """
        {"jsonrpc": "2.0","id": 1,"method":"resources/list"}""";
    public static final String RESOURCES_LIST_RESULT = """
{"jsonrpc": "2.0","id": 1,"result": {"resources":[]}}""";

    /**
     * Even if the server does not have the resource capabilities, clients may not respect that and send a resource/list request.
     * In that scenario, the server should return an empty list of resources
     * @param httpClient HTTP Client
     * @throws JSONException JSON Exception
     */
    @Test
    void testResourceList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_LIST);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        String expected = RESOURCES_LIST_RESULT;
        JSONAssert.assertEquals(expected, jsonRpc, true);
    }
}
