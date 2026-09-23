package io.micronaut.mcp.docs.completions.prompts;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPT_COMPLETION;
import static io.micronaut.mcp.docs.JsonRpcMessages.PROMPT_COMPLETION_REQUEST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyPromptsCompletionsTest")
@MicronautTest
class MyPromptsCompletionsTest {

    @Test
    void promptCompletion(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", PROMPT_COMPLETION_REQUEST)));
        JSONAssert.assertEquals(EXPECTED_PROMPT_COMPLETION, result, true);
    }
}
