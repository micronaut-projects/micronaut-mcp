package io.micronaut.mcp.server.context;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
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
class MicronautMcpTransportContextResourcesTest {
    public static final String RESOURCES_GET = """
        { "jsonrpc": "2.0", "id": 2,"method": "resources/read","params": {"uri": "https://example.com/locale"}}""";

    @Test
    void methodsAnnotatedWithPromptCanBeBoundMcpTransportContext(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_GET);
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        String expected = """
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "contents": [
      {
        "uri": "https://example.com/locale",
        "mimeType": "text/plain",
        "text": "https://example.com/locale/es-ES"
      }
    ]
  }
}""";
        JSONAssert.assertEquals(expected, responseJson, true);
    }

    @Requires(property = "spec.name", value = "MicronautMcpTransportContextTest")
    @Singleton
    static class Resources {
        @Resource(uri = "https://example.com/locale",
            name = "locale",
            title = "Locale Resource",
            description = "Returns the locale from the McpTransportContext")
        public String locale(MicronautMcpTransportContext context, McpSchema.ReadResourceRequest request) {
            return request.uri() + "/" + context.locale().toLanguageTag();
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
