package io.micronaut.mcp.server.context;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import org.jspecify.annotations.NonNull;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
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
class MicronautMcpTransportContextPromptsTest {
    public static final String PROMPTS_GET = """
        {"jsonrpc":"2.0","id":0,"method": "prompts/get","params":{"name":"chess-statistics","arguments":{"firstName": "Sergio", "lastName": "del Amo"}}}""";

    @Test
    void methodsAnnotatedWithPromptCanBeBoundMcpTransportContext(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", PROMPTS_GET);
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        String expected = """
            {"jsonrpc":"2.0","id":0,"result":{"description":"","messages":[{"role":"user","content":{"type":"text","text":"Sergio del Amo es-ES chess-statistics"}}]}}""";
        JSONAssert.assertEquals(expected, responseJson, true);
    }

    @Requires(property = "spec.name", value = "MicronautMcpTransportContextTest")
    @Singleton
    static class Prompts {
        @Prompt(name = "chess-statistics", description = "Chess statistics description")
        McpSchema.GetPromptResult chessStats(@PromptArg(description = "First Name") @NonNull String firstName,
                                             MicronautMcpTransportContext context,
                                             @PromptArg(description = "Last Name", name = "lastName") @NonNull String familyName,
                                             McpSchema.GetPromptRequest request) {
            return new McpSchema.GetPromptResult("",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(String.join(" ", firstName, familyName, context.locale().toLanguageTag(), request.name())))),
                null);
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
