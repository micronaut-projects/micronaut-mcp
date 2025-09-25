package io.micronaut.mcp.server.context;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.tools.search.SearchRequest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautMcpTransportContextToolBindingTest")
@Property(name = "micronaut.server.locale-resolution.fixed", value = "es_ES")
@MicronautTest
class MicronautMcpTransportContextToolBindingTest {

    @Test
    void localeToolWithPojo(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "translatorPojo",
                "arguments": {
                "query": "Hello"
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
        "text": "Hola"
      }
    ],
    "isError": false
  }
}""";
        JSONAssert.assertEquals(expected, responseJson, true);
    }


    @Test
    void localeToolWithMultipleArguments(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {
              "method": "tools/call",
              "params": {
                "name": "translator",
                "arguments": {
                "query": "Hello"
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
        "text": "Hola"
      }
    ],
    "isError": false
  }
}""";
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

    @Requires(property = "spec.name", value = "MicronautMcpTransportContextToolBindingTest")
    @Singleton
    static class Tools {
        @Tool
        String locale(MicronautMcpTransportContext ctx) {
            Locale locale = ctx.locale();
            if (locale != null) {
                return locale.toLanguageTag();
            }
            throw new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, "could not get the locale", null));
        }

        @Tool
        String translator(String query, MicronautMcpTransportContext ctx) {
            Locale locale = ctx.locale();
            if (locale != null) {
                if (query.equalsIgnoreCase("hello")) {
                    if (locale.toLanguageTag().equals("es-ES")) {
                        return "Hola";
                    }
                    return "Hello";
                }
                return "no idea";
            }
            throw new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, "could not get the locale", null));
        }

        @Tool
        String translatorPojo(MicronautMcpTransportContext ctx, SearchRequest request) {
            Locale locale = ctx.locale();
            if (locale != null) {
                if (request.query().equalsIgnoreCase("hello")) {
                    if (locale.toLanguageTag().equals("es-ES")) {
                        return "Hola";
                    }
                    return "Hello";
                }
                return "no idea";
            }
            throw new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, "could not get the locale", null));
        }
    }
}
