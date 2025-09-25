package io.micronaut.mcp.server.tools.fetch;

import io.micronaut.context.annotation.Property;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "FetchToolFactoryTest")
@Property(name = "spec.tool.name", value = "MicronautModulesFetch")
@MicronautTest
class FetchToolFactoryHttpTest {

    @Test
    void fetchTool(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "fetch",
                "arguments": {
                  "id": "micronaut-security"
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
         "text": "{\\"id\\":\\"micronaut-security\\",\\"title\\":\\"Micronaut Security\\",\\"text\\":\\"Built-in security features. Authentication providers and strategies, Token Propagation.\\",\\"url\\":\\"https://micronaut-projects.github.io/micronaut-security/latest/guide\\"}"
       }
     ],
     "isError": false,
     "structuredContent": {
       "id": "micronaut-security",
       "title": "Micronaut Security",
       "text": "Built-in security features. Authentication providers and strategies, Token Propagation.",
       "url": "https://micronaut-projects.github.io/micronaut-security/latest/guide"
     }
   }
 }""";
        JSONAssert.assertEquals(expected, responseJson, true);
    }
}
