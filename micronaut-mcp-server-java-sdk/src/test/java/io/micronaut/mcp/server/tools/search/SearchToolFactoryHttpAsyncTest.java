package io.micronaut.mcp.server.tools.search;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "SearchToolFactoryTest")
@MicronautTest
class SearchToolFactoryHttpAsyncTest {

    @Test
    void searchTool(@Client("/") HttpClient httpClient) throws JSONException {
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

    @Requires(property = "spec.name", value = "SearchToolFactoryHttpAsyncTest")
    @Singleton
    static class MicronautModulesSearch implements SearchTool {

        @Override
        public SearchResponse search(SearchRequest request, McpTransportContext transportContext) {
            return new SearchResponse(List.of(SearchResult.builder()
                .id("micronaut-security")
                .title("Micronaut Security")
                .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
                .build()));
        }
    }


}
