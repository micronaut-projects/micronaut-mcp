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

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST_ANNOTATIONS;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_READ_HELLO;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_READ_HELLO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "StatelessSyncResourcesAnnotationsTest")
@MicronautTest
class StatelessSyncResourcesAnnotationsTest {

    @Test
    void resourcesList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_LIST);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_ANNOTATIONS, result, true);
    }

    @Test
    void resourcesRead(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_READ_HELLO);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ_HELLO, result, true);
    }
}
