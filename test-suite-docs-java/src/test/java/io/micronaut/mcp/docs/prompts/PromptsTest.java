package io.micronaut.mcp.docs.prompts;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPTS_GET;
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPTS_LIST;
import static io.micronaut.mcp.docs.JsonRpcMessages.PROMPTS_GET;
import static io.micronaut.mcp.docs.JsonRpcMessages.PROMPTS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "PromptsTest")
@MicronautTest
class PromptsTest {

    @Test
    void promptsList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", PROMPTS_LIST)));
        JSONAssert.assertEquals(EXPECTED_PROMPTS_LIST, result, true);
    }

    @Test
    void promptsGet(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", PROMPTS_GET)));
        JSONAssert.assertEquals(EXPECTED_PROMPTS_GET, result, true);
    }
}
