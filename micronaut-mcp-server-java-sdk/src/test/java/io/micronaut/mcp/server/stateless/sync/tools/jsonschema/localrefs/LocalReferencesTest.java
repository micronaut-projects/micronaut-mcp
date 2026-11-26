package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.localrefs;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The references local to the JSON Schema of a ToolArg type keep resolving within that schema once it is embedded in the
 * tool input schema, when the SDK validates the tool arguments.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "LocalReferencesTest")
@MicronautTest
class LocalReferencesTest {

    @Test
    void validNestedValueIsAccepted(@Client("/") HttpClient httpClient) throws JSONException {
        JSONObject result = call(httpClient, """
            {"node":{"name":"a","child":{"name":"b"}}}""");
        assertEquals(false, result.getBoolean("isError"), result::toString);
        assertEquals("a/b", text(result));
    }

    @Test
    void invalidNestedValueIsRejected(@Client("/") HttpClient httpClient) throws JSONException {
        JSONObject result = call(httpClient, """
            {"node":{"name":"a","child":{"name":"toolong"}}}""");
        assertTrue(result.getBoolean("isError"), result::toString);
        assertTrue(text(result).contains("input validation failed"), result::toString);
    }

    @Test
    void selfReferenceResolvesWithinTheEmbeddedSchema(@Client("/") HttpClient httpClient) throws JSONException {
        JSONObject valid = call(httpClient, "treeNames", """
            {"tree":{"name":"a","child":{"name":"b"}}}""");
        assertEquals(false, valid.getBoolean("isError"), valid::toString);
        assertEquals("a/b", text(valid));

        // Were "#" resolved against the tool input schema, this value would be accepted
        JSONObject result = call(httpClient, "treeNames", """
            {"tree":{"name":"a","child":{"tree":{"name":"b"},"evil":"x","name":123456}}}""");
        assertTrue(result.getBoolean("isError"), result::toString);
        assertTrue(text(result).contains("input validation failed"), result::toString);
    }

    private static String text(JSONObject result) throws JSONException {
        return result.getJSONArray("content").getJSONObject(0).getString("text");
    }

    private static JSONObject call(HttpClient httpClient, String arguments) throws JSONException {
        return call(httpClient, "names", arguments);
    }

    private static JSONObject call(HttpClient httpClient, String tool, String arguments) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"%s","arguments":%s}}""".formatted(tool, arguments));
        return new JSONObject(assertDoesNotThrow(() -> client.retrieve(req))).getJSONObject("result");
    }
}
