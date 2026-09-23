package io.micronaut.mcp.docs.tools;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS;
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "HelloWorldToolTest")
@MicronautTest
class HelloWorldToolTest {

    @Test
    void toolAnnotations(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        String json = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)));
        assertTrue(json.contains(EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS), json);
    }
}
