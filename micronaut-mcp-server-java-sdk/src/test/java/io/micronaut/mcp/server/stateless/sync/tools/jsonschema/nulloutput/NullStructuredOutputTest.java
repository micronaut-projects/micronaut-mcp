package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.nulloutput;

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

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "NullStructuredOutputTest")
@MicronautTest
class NullStructuredOutputTest {

    @Test
    void nullStructuredResultIsToolError(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"unknownEvaluation","arguments":{}}}""");
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"Tool unknownEvaluation returned no structured content"}],"isError":true}}""",
            result, true);
    }
}
