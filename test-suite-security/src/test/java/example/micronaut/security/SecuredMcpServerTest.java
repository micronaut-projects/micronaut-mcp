package example.micronaut.security;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "secured")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.security.token.jwt.signatures.secret.generator.secret", value = "pleaseChangeThisSecretForANewOneAtLeast256BitsLong")
@Property(name = "micronaut.security.intercept-url-map[0].pattern", value = "/mcp")
@Property(name = "micronaut.security.intercept-url-map[0].access[0]", value = "isAuthenticated()")
@Property(name = "spec.name", value = "SecuredMcpServerTest")
@MicronautTest
class SecuredMcpServerTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    TokenGenerator tokenGenerator;

    @Test
    void anUnauthenticatedCallIsChallengedWithTheProtectedResourceMetadata() {
        BlockingHttpClient client = httpClient.toBlocking();
        MutableHttpRequest<String> request = whoami();
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        String challenge = ex.getResponse().getHeaders().get("WWW-Authenticate");
        assertTrue(challenge != null && challenge.startsWith("Bearer") && challenge.contains("resource_metadata=")
            && challenge.contains("/.well-known/oauth-protected-resource/mcp"), challenge);
    }

    @Test
    void theProtectedResourceMetadataNamesTheMcpEndpoint() {
        String metadata = httpClient.toBlocking().retrieve("/.well-known/oauth-protected-resource/mcp");
        assertTrue(metadata.contains("\"resource\""), metadata);
        assertTrue(metadata.contains("/mcp\""), metadata);
    }

    @Test
    void anAuthenticatedCallReachesTheToolWithItsPrincipal() throws JSONException {
        String token = tokenGenerator.generateToken(Authentication.build("alice", List.of("ROLE_USER")), 3600).orElseThrow();
        HttpResponse<String> response = httpClient.toBlocking().exchange(whoami().bearerAuth(token), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"result": {"content": [{"type": "text", "text": "alice"}], "isError": false}}""", response.body(), JSONCompareMode.LENIENT);
    }

    private static MutableHttpRequest<String> whoami() {
        return HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "whoami", "arguments": {}}}""");
    }

    @Requires(property = "spec.name", value = "SecuredMcpServerTest")
    @Singleton
    static class Tools {
        @Tool
        String whoami(MicronautMcpTransportContext context) {
            return context.principal() != null ? context.principal().getName() : "anonymous";
        }
    }
}
