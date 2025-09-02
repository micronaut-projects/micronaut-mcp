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

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_PROMPTS;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PROMPTS_GET;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PROMPTS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "StatelessSyncPromptsAnnotationsTest")
@MicronautTest
class StatelessSyncPromptsAnnotationsTest {

    @Test
    void statelessSyncPromptsList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> listRequest = HttpRequest.POST("/mcp", PROMPTS_LIST);
        String listResult = assertDoesNotThrow(() -> client.retrieve(listRequest));
        JSONAssert.assertEquals(EXPECTED_PROMPTS, listResult, true);

        HttpRequest<?> getRequest = HttpRequest.POST("/mcp", PROMPTS_GET);
        String getResult = assertDoesNotThrow(() -> client.retrieve(getRequest));
        assertNotNull(getResult);
    }
}
