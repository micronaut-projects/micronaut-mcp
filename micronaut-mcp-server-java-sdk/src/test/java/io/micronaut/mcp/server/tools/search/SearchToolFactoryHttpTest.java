package io.micronaut.mcp.server.tools.search;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.*;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "SearchToolFactoryTest")
@Property(name = "spec.tool.name", value = "MicronautModulesSearch")
@MicronautTest
class SearchToolFactoryHttpTest {

    @Test
    void searchTool(@Client("/") HttpClient httpClient, SearchTool tool) throws JSONException {
        assertEquals("search", tool.getName());
        assertEquals("Search", tool.getTitle());
        assertEquals("Returns a list of relevant search results, given a user's query.", tool.getDescription());
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "search",
                "arguments": {
                  "query": "security"
                }
              },
              "jsonrpc": "2.0",
              "id": 20
            }""");
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        String expected = """

            {
  "jsonrpc": "2.0",
  "id": 20,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\\"results\\":[{\\"id\\":\\"micronaut-security\\",\\"title\\":\\"Micronaut Security\\",\\"url\\":\\"https://micronaut-projects.github.io/micronaut-security/latest/guide\\"}]}"
      }
    ],
    "isError": false,
    "structuredContent": {
      "results": [
        {
          "id": "micronaut-security",
          "title": "Micronaut Security",
          "url": "https://micronaut-projects.github.io/micronaut-security/latest/guide"
        }
      ]
    }
  }
}""";
        JSONAssert.assertEquals(expected, responseJson, true);
    }
}
