package io.micronaut.mcp.docs.tools.search;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.server.tools.search.SearchTool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_SEARCH_TOOL_CALL;
import static io.micronaut.mcp.docs.JsonRpcMessages.SEARCH_TOOL_CALL;
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautModulesSearchTest")
@MicronautTest
class MicronautModulesSearchTest {

    @Test
    void searchTool(@Client("/") HttpClient httpClient, SearchTool tool) throws JSONException {
        assertEquals("search", tool.getName());
        assertEquals("Search", tool.getTitle());
        BlockingHttpClient client = httpClient.toBlocking();
        String json = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)));
        assertTrue(json.contains("\"name\":\"search\""), json);
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", SEARCH_TOOL_CALL)));
        JSONAssert.assertEquals(EXPECTED_SEARCH_TOOL_CALL, result, true);
    }
}
