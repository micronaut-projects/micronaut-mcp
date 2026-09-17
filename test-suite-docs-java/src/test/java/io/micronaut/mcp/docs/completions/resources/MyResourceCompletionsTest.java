package io.micronaut.mcp.docs.completions.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_READ_REPORT;
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCE_COMPLETION;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_REPORT;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCE_COMPLETION_REQUEST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyResourceCompletionsTest")
@MicronautTest
class MyResourceCompletionsTest {

    @Test
    void resourceCompletion(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCE_COMPLETION_REQUEST)));
        JSONAssert.assertEquals(EXPECTED_RESOURCE_COMPLETION, result, true);
    }

    @Test
    void resourceTemplateRead(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_REPORT)));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ_REPORT, result, true);
    }
}
