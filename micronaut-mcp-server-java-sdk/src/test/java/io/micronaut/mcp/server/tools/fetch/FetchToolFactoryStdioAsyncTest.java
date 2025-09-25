package io.micronaut.mcp.server.tools.fetch;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@Property(name = "spec.name", value = "FetchToolFactoryStdioAsyncTest")
@Property(name = "spec.tool.name", value = "MicronautModulesFetch")
@MicronautTest
class FetchToolFactoryStdioAsyncTest {
    @Inject
    MockMcpServerTransportProvider factory;

    @Test
    void fetchTool() throws JSONException, IOException, InterruptedException {
        factory.stdio.sendRequest(INITIALIZE);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(INITIALIZED);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest("{\"method\": \"tools/call\",\"params\": {\"name\": \"fetch\",\"arguments\": {\"id\": \"micronaut-security\"}},\"jsonrpc\": \"2.0\",\"id\": 20}");
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

    @Requires(property = "spec.name", value = "FetchToolFactoryStdioAsyncTest")
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
