package io.micronaut.mcp.server.stateless.sync.prompts;

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
class PromptsListNoPromptsCapabilityTest {
    public static final String PROMPTS_LIST = """
        {"jsonrpc": "2.0","id": 1,"method":"prompts/list","params":{}}""";
    public static final String PROMPTS_LIST_RESULT = """
{"jsonrpc": "2.0","id": 1,"result": {"prompts":[]}}""";

    /**
     * Even if the server does not have the prompts capabilities, clients may not respect that and send a prompts/list request.
     * In that scenario, the server should return an empty list of prompts
     * @param httpClient HTTP Client
     * @throws JSONException JSON Exception
     */
    @Test
    void testResourceList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", PROMPTS_LIST);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        String expected = PROMPTS_LIST_RESULT;
        JSONAssert.assertEquals(expected, jsonRpc, true);
    }
}
