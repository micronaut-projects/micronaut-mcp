package example.micronaut.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the configuration of the Authorization section of the guide against a stub identity provider.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "secured")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.security.intercept-url-map[0].pattern", value = "/mcp")
@Property(name = "micronaut.security.intercept-url-map[0].access[0]", value = "isAuthenticated()")
@Property(name = "micronaut.security.token.jwt.claims-validators.audience", value = SecuredMcpServerTest.AUDIENCE)
// Core denies non-local origins on a localhost server; pass them through to reach the MCP endpoint rules
@Property(name = "micronaut.server.cors.localhost-pass-through", value = StringUtils.TRUE)
@Property(name = "spec.name", value = "SecuredMcpServerTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecuredMcpServerTest implements TestPropertyProvider {

    private static final String KEY_ID = "idp";
    static final String AUDIENCE = "https://mcp.example.com/mcp";

    private final RSAKey key = generateKey();

    private final EmbeddedServer idp = ApplicationContext.run(EmbeddedServer.class, Map.of(
        "spec.name", "SecuredMcpServerTestIdp",
        "micronaut.security.enabled", StringUtils.FALSE,
        "micronaut.mcp.server.enabled", StringUtils.FALSE,
        "idp.jwks", new JWKSet(key.toPublicJWK()).toString()));

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    EmbeddedServer server;

    @Override
    public Map<String, String> getProperties() {
        String issuer = issuer();
        return Map.of(
            "micronaut.security.token.jwt.signatures.jwks.idp.url", issuer + "/keys",
            "micronaut.security.oauth2.clients.idp.openid.issuer", issuer);
    }

    @AfterAll
    void stopIdp() {
        idp.close();
    }

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
    void theProtectedResourceMetadataNamesTheMcpEndpointAndTheAuthorizationServer() throws JSONException {
        String metadata = httpClient.toBlocking().retrieve("/.well-known/oauth-protected-resource/mcp");
        JSONAssert.assertEquals("""
            {"resource": "%s/mcp", "authorization_servers": ["%s"]}""".formatted(server.getURL(), issuer()),
            metadata, JSONCompareMode.LENIENT);
    }

    @Test
    void aCallWithATokenOfTheIdentityProviderReachesTheToolWithItsPrincipal() throws JSONException, JOSEException {
        HttpResponse<String> response = httpClient.toBlocking().exchange(whoami().bearerAuth(token("alice", AUDIENCE)), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"result": {"content": [{"type": "text", "text": "alice"}], "isError": false}}""", response.body(), JSONCompareMode.LENIENT);
    }

    @Test
    void aTokenIssuedForAnotherResourceIsRejected() throws JOSEException {
        BlockingHttpClient client = httpClient.toBlocking();
        MutableHttpRequest<String> request = whoami().bearerAuth(token("alice", "https://other.example.com/api"));
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void aForeignOriginIsRejectedBeforeAuthentication() {
        BlockingHttpClient client = httpClient.toBlocking();
        MutableHttpRequest<String> request = whoami().header("Origin", "http://evil.example");
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request, String.class));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        String body = ex.getResponse().getBody(String.class).orElseThrow();
        assertTrue(body.contains("Origin not allowed: http://evil.example"), body);
    }

    private String issuer() {
        return idp.getURL().toString();
    }

    private String token(String subject, String audience) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
            new JWTClaimsSet.Builder()
                .issuer(issuer())
                .subject(subject)
                .audience(audience)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
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

    /**
     * The identity provider: its OpenID Connect discovery document and its JSON Web Key Set.
     */
    @Requires(property = "spec.name", value = "SecuredMcpServerTestIdp")
    @Controller
    static class IdentityProvider {
        private final EmbeddedServer server;
        private final String jwks;

        IdentityProvider(EmbeddedServer server, @Value("${idp.jwks}") String jwks) {
            this.server = server;
            this.jwks = jwks;
        }

        @Get(uri = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON)
        String openIdConfiguration() {
            String issuer = server.getURL().toString();
            return """
                {"issuer": "%1$s", "jwks_uri": "%1$s/keys", "authorization_endpoint": "%1$s/authorize",
                 "token_endpoint": "%1$s/token", "response_types_supported": ["code"],
                 "subject_types_supported": ["public"], "id_token_signing_alg_values_supported": ["RS256"]}"""
                .formatted(issuer);
        }

        @Get(uri = "/keys", produces = MediaType.APPLICATION_JSON)
        String keys() {
            return jwks;
        }
    }
}
