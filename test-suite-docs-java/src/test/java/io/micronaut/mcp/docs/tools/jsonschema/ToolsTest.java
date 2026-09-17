package io.micronaut.mcp.docs.tools.jsonschema;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_TOOLS_CALL;
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_TOOLS_LIST_WITH_INPUT_SCHEMA;
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_CALL;
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "JsonSchemaToolsTest")
@MicronautTest
class ToolsTest {

    @Test
    void toolsList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)));
        JSONAssert.assertEquals(EXPECTED_TOOLS_LIST_WITH_INPUT_SCHEMA, result, true);
    }

    @Test
    void toolsCall(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", TOOLS_CALL)));
        JSONAssert.assertEquals(EXPECTED_TOOLS_CALL, result, true);
    }
}
