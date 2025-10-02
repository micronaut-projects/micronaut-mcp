package io.micronaut.mcp.server.stateless.sync.completions.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCE_COMPLETION_REQUEST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCE_COMPLETION_RESPONSE;
import static io.modelcontextprotocol.spec.McpSchema.Role.USER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest
@Property(name = "spec.name", value = "StatelessSyncResourceCompletionsTest")
class StatelessSyncResourceCompletionsTest {

    @Test
    void resourceCompletion(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCE_COMPLETION_REQUEST);
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        JSONAssert.assertEquals(RESOURCE_COMPLETION_RESPONSE, responseJson, true);
    }
}
