package io.micronaut.mcp.docs.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_TEMPLATES_LIST;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_2;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_99;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_TEMPLATES_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyResourcesTemplatesTest")
@MicronautTest
class MyResourcesTemplatesTest {

    @Test
    void resourcesTemplatesList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST)));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_TEMPLATES_LIST, result, true);
    }

    @Test
    void resourcesRead(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_2)));
        assertTrue(result.contains("\"uri\":\"pgn://round/2\""), result);
        assertTrue(result.contains("\"mimeType\":\"application/x-chess-pgn\""), result);
        assertTrue(result.contains("[Round \\\"2\\\"]"), result);
    }

    @Test
    void resourcesReadNotFound(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
            () -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_99)));
        String body = ex.getResponse().getBody(String.class).orElse("");
        assertTrue(body.contains("\"error\""), body);
        assertTrue(body.contains("resource for round not found"), body);
    }
}
