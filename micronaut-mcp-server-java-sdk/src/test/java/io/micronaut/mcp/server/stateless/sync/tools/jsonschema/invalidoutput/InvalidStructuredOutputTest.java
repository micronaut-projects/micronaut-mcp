package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.invalidoutput;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structured results that the SDK rejects, with the Micronaut MCP JSON mapper.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "InvalidStructuredOutputTest")
@MicronautTest
class InvalidStructuredOutputTest {

    @Test
    void resultNotMatchingTheOutputSchemaIsToolError(@Client("/") HttpClient httpClient) throws JSONException {
        JSONObject result = new JSONObject(call(httpClient, "blankEvaluation")).getJSONObject("result");
        assertTrue(result.getBoolean("isError"));
        assertFalse(result.has("structuredContent"));
        String text = result.getJSONArray("content").getJSONObject(0).getString("text");
        assertTrue(text.startsWith("Tool (blankEvaluation) output validation failed: "
            + "Validation failed: structuredContent does not match tool outputSchema."), text);
    }

    @Test
    void resultThatCannotBeSerializedIsToolErrorWithoutTheCause(@Client("/") HttpClient httpClient) throws JSONException {
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"Tool (unserializableEvaluation) output validation failed: Structured content cannot be serialized"}],"isError":true}}""",
            call(httpClient, "unserializableEvaluation"), true);
    }

    private static String call(HttpClient httpClient, String tool) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"%s","arguments":{}}}""".formatted(tool));
        return assertDoesNotThrow(() -> client.retrieve(req));
    }
}
