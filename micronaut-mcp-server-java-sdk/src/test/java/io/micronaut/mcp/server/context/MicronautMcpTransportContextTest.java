package io.micronaut.mcp.server.context;

import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.server.EmbeddedServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.json.JSONException;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautMcpTransportContextTest")
@Property(name = "micronaut.server.locale-resolution.fixed", value = "es_ES")
@MicronautTest
class MicronautMcpTransportContextTest {

    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void lastEventIdInContext(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "lastEventId",
                "arguments": {}
              },
              "jsonrpc": "2.0",
              "id": 20
            }""").header("Last-Event-ID", "4578");
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        String expected = String.format("""

            {
  "jsonrpc": "2.0",
  "id": 20,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "4578"
      }
    ],
    "isError": false
  }
}""", embeddedServer.getPort());
        JSONAssert.assertEquals(expected, responseJson, true);
    }

    @Test
    void sessionIdInContext(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "sessionId",
                "arguments": {}
              },
              "jsonrpc": "2.0",
              "id": 20
            }""").header("Mcp-Session-Id", "123456789");
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        String expected = String.format("""

            {
  "jsonrpc": "2.0",
  "id": 20,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "123456789"
      }
    ],
    "isError": false
  }
}""", embeddedServer.getPort());
        JSONAssert.assertEquals(expected, responseJson, true);
    }

    @Test
    void protocolVersionInContext(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "protocolVersion",
                "arguments": {}
              },
              "jsonrpc": "2.0",
              "id": 20
            }""").header("MCP-Protocol-Version", "2025-06-18");
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        String expected = String.format("""

            {
  "jsonrpc": "2.0",
  "id": 20,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "2025-06-18"
      }
    ],
    "isError": false
  }
}""", embeddedServer.getPort());
        JSONAssert.assertEquals(expected, responseJson, true);
    }

    @Test
    void hostTool(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "host",
                "arguments": {}
              },
              "jsonrpc": "2.0",
              "id": 20
            }""");
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        String expected = String.format("""

            {
  "jsonrpc": "2.0",
  "id": 20,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "http://localhost:%s"
      }
    ],
    "isError": false
  }
}""", embeddedServer.getPort());
        JSONAssert.assertEquals(expected, responseJson, true);
    }

    @Test
    void localeTool(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "locale",
                "arguments": {}
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
        "text": "es-ES"
      }
    ],
    "isError": false
  }
}""";
        JSONAssert.assertEquals(expected, responseJson, true);
    }

    @Test
    void userTool(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "user",
                "arguments": {}
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
        "text": "user: sdelamo role: [ROLE_USER]"
      }
    ],
    "isError": false
  }
}""";
        JSONAssert.assertEquals(expected, responseJson, true);
    }


    @Requires(property = "spec.name", value = "MicronautMcpTransportContextTest")
    @Factory
    static class ToolsFactory {
        @Singleton
        McpStatelessServerFeatures.SyncToolSpecification hostTool() {
            return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                    .name("host")
                    .build())
                .callHandler((exchange, req) -> {
                    if (exchange instanceof MicronautMcpTransportContext context) {
                        return McpSchema.CallToolResult.builder()
                            .addTextContent(context.host())
                            .build();
                    } else {
                        return McpSchema.CallToolResult.builder()
                            .isError(true)
                            .build();
                    }
                })
                .build();
        }

        @Singleton
        McpStatelessServerFeatures.SyncToolSpecification localeTool() {
            return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                    .name("locale")
                    .build())
                .callHandler((exchange, req) -> {
                    if (exchange instanceof MicronautMcpTransportContext context) {
                        return McpSchema.CallToolResult.builder()
                            .addTextContent(context.locale().toLanguageTag())
                            .build();
                    } else {
                        return McpSchema.CallToolResult.builder()
                            .isError(true)
                            .build();
                    }
                })
                .build();
        }

        @Singleton
        McpStatelessServerFeatures.SyncToolSpecification protocolVersionTool() {
            return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                    .name("protocolVersion")
                    .build())
                .callHandler((exchange, req) -> {
                    if (exchange instanceof MicronautMcpTransportContext context) {
                        return McpSchema.CallToolResult.builder()
                            .addTextContent(context.protocolVersion())
                            .build();
                    } else {
                        return McpSchema.CallToolResult.builder()
                            .isError(true)
                            .build();
                    }
                })
                .build();
        }

        @Singleton
        McpStatelessServerFeatures.SyncToolSpecification lastEventIdTool() {
            return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                    .name("lastEventId")
                    .build())
                .callHandler((exchange, req) -> {
                    if (exchange instanceof MicronautMcpTransportContext context) {
                        return McpSchema.CallToolResult.builder()
                            .addTextContent(context.lastEventId())
                            .build();
                    } else {
                        return McpSchema.CallToolResult.builder()
                            .isError(true)
                            .build();
                    }
                })
                .build();
        }

        @Singleton
        McpStatelessServerFeatures.SyncToolSpecification sessionIdTool() {
            return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                    .name("sessionId")
                    .build())
                .callHandler((exchange, req) -> {
                    if (exchange instanceof MicronautMcpTransportContext context) {
                        return McpSchema.CallToolResult.builder()
                            .addTextContent(context.sessionId())
                            .build();
                    } else {
                        return McpSchema.CallToolResult.builder()
                            .isError(true)
                            .build();
                    }
                })
                .build();
        }

        @Singleton
        McpStatelessServerFeatures.SyncToolSpecification userTool() {
            return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                    .name("user")
                    .build())
                .callHandler((exchange, req) -> {
                    if (exchange instanceof MicronautMcpTransportContext context) {
                        if (context.principal() instanceof Authentication authentication) {
                            return McpSchema.CallToolResult.builder()
                                .addTextContent("user: " + authentication.getName() + " role: " + authentication.getRoles())
                                .build();
                        } else {
                            return McpSchema.CallToolResult.builder()
                                .addTextContent("user: " + context.principal().getName())
                                .build();
                        }
                    } else {
                        return McpSchema.CallToolResult.builder()
                            .isError(true)
                            .build();
                    }
                })
                .build();
        }
    }

    @Requires(property = "spec.name", value = "MicronautMcpTransportContextTest")
    @Singleton
    static class TestAuthenticationFetcher implements AuthenticationFetcher<HttpRequest<?>> {
        @Override
        public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
            return Publishers.just(Authentication.build("sdelamo", List.of("ROLE_USER")));
        }
    }
}
