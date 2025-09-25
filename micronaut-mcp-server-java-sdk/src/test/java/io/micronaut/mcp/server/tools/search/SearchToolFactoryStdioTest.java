package io.micronaut.mcp.server.tools.search;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "spec.name", value = "SearchToolFactoryStdioTest")
@Property(name = "spec.tool.name", value = "MicronautModulesSearch")
@MicronautTest
class SearchToolFactoryStdioTest {
    @Inject
    MockMcpServerTransportProvider factory;

    @Test
    void searchTool() throws JSONException, IOException, InterruptedException {
        factory.stdio.sendRequest(INITIALIZE);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(INITIALIZED);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest("{\"method\": \"tools/call\",\"params\": {\"name\": \"search\",\"arguments\": {\"query\": \"security\"}},\"jsonrpc\": \"2.0\",\"id\": 20}");
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        List<String> responses = factory.stdio.readResponses();
        assertEquals(2, responses.size());
        String responseJson = responses.get(1);
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

    @Requires(property = "spec.name", value = "SearchToolFactoryStdioTest")
    @Factory
    static class MockMcpServerTransportProvider implements AutoCloseable {
        public final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(McpJsonMapper jsonMapper) {
            return new StdioServerTransportProvider(jsonMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() {
            stdio.close();
        }
    }

}
