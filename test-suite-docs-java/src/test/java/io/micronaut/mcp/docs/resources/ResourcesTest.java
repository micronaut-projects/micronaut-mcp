package io.micronaut.mcp.docs.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_LIST_HELLO;
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_READ_HELLO;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_LIST;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_HELLO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ResourcesTest")
@MicronautTest
class ResourcesTest {

    @Test
    void resourcesList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST)));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_HELLO, result, true);
    }

    @Test
    void resourcesRead(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_HELLO)));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ_HELLO, result, true);
    }
}
