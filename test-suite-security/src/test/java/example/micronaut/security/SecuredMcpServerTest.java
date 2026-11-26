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
import io.micronaut.core.io.socket.SocketUtils;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the configuration of the Authorization section of the guide
 * ({@code src/main/docs/guide/server/security/authorization.adoc}) against a stub identity provider. Keep the two in
 * sync: the {@code intercept-url-map} below and the JWKS, issuer and audience properties of {@link #getProperties()} are
 * the guide's {@code [configuration]} block, with the stub's URLs and the server's own URL as the audience.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "secured")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.security.intercept-url-map[0].pattern", value = "/mcp")
@Property(name = "micronaut.security.intercept-url-map[0].access[0]", value = "isAuthenticated()")
// Core denies non-local origins on a localhost server; pass them through to reach the MCP endpoint rules
@Property(name = "micronaut.server.cors.localhost-pass-through", value = StringUtils.TRUE)
@Property(name = "spec.name", value = "SecuredMcpServerTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecuredMcpServerTest implements TestPropertyProvider {

    private static final String KEY_ID = "idp";

    private final RSAKey key = generateKey();

    private final EmbeddedServer idp = ApplicationContext.run(EmbeddedServer.class, Map.of(
        "spec.name", "SecuredMcpServerTestIdp",
        "micronaut.security.enabled", StringUtils.FALSE,
        "micronaut.mcp.server.enabled", StringUtils.FALSE,
        "idp.jwks", new JWKSet(key.toPublicJWK()).toString()));

    // The server's port is chosen up front, so that the audience can be the resource the metadata reports
    private final int port = SocketUtils.findAvailableTcpPort();

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Override
    public Map<String, String> getProperties() {
        String issuer = issuer();
        return Map.of(
            "micronaut.server.port", String.valueOf(port),
            "micronaut.security.token.jwt.claims-validators.audience", audience(),
            "micronaut.security.token.jwt.signatures.jwks.idp.url", issuer + "/.well-known/jwks.json",
            "micronaut.security.oauth2.clients.idp.openid.issuer", issuer);
    }

    @AfterAll
    void stopIdp() {
        idp.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mcp", "/mcp/"})
    void anUnauthenticatedCallIsChallengedWithTheProtectedResourceMetadata(String path) {
        HttpClientResponseException ex = rejected(whoami(path));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Bearer resource_metadata=\"http://localhost:" + port + "/.well-known/oauth-protected-resource"
            + path + "\"", ex.getResponse().getHeaders().get("WWW-Authenticate"));
    }

    @Test
    void theProtectedResourceMetadataNamesTheAudienceAndTheAuthorizationServer() throws JSONException {
        JSONObject metadata = new JSONObject(httpClient.toBlocking().retrieve("/.well-known/oauth-protected-resource/mcp"));
        // The guide asks for the audience to be the resource the metadata reports
        assertEquals(audience(), metadata.getString("resource"));
        JSONAssert.assertEquals(new JSONArray(List.of(issuer())), metadata.getJSONArray("authorization_servers"), JSONCompareMode.STRICT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mcp", "/mcp/"})
    void aCallWithATokenOfTheIdentityProviderReachesTheToolWithItsPrincipal(String path) throws JSONException, JOSEException {
        HttpResponse<String> response = httpClient.toBlocking().exchange(whoami(path).bearerAuth(token(claims().build(), key)), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"result": {"content": [{"type": "text", "text": "alice"}], "isError": false}}""", response.body(), JSONCompareMode.LENIENT);
    }

    @Test
    void aTokenWithSeveralAudiencesIncludingTheServerIsAccepted() throws JOSEException {
        String token = token(claims().audience(List.of("https://other.example.com/api", audience())).build(), key);
        assertEquals(HttpStatus.OK, httpClient.toBlocking().exchange(whoami("/mcp").bearerAuth(token), String.class).getStatus());
    }

    @Test
    void aTokenIssuedForAnotherResourceIsRejected() throws JOSEException {
        String token = token(claims().audience("https://other.example.com/api").build(), key);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp/").bearerAuth(token)).getStatus());
    }

    @Test
    void aTokenWithoutAnAudienceIsRejected() throws JOSEException {
        String token = token(claims().audience((String) null).build(), key);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
    }

    @Test
    void anExpiredTokenIsRejected() throws JOSEException {
        String token = token(claims().expirationTime(Date.from(Instant.now().minus(1, ChronoUnit.HOURS))).build(), key);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() throws JOSEException {
        String token = token(claims().build(), generateKey());
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
    }

    @Test
    void aForeignOriginIsRejectedBeforeAuthentication() {
        HttpClientResponseException ex = rejected(whoami("/mcp").header("Origin", "http://evil.example"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        String body = ex.getResponse().getBody(String.class).orElseThrow();
        assertTrue(body.contains("Origin not allowed: http://evil.example"), body);
    }

    private HttpClientResponseException rejected(MutableHttpRequest<String> request) {
        BlockingHttpClient client = httpClient.toBlocking();
        return assertThrows(HttpClientResponseException.class, () -> client.exchange(request, String.class));
    }

    private String audience() {
        return "http://localhost:" + port + "/mcp";
    }

    private String issuer() {
        return idp.getURL().toString();
    }

    /**
     * The claims of a valid token for alice.
     */
    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder()
            .issuer(issuer())
            .subject("alice")
            .audience(audience())
            .issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
    }

    private static String token(JWTClaimsSet claims, RSAKey signingKey) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MutableHttpRequest<String> whoami(String path) {
        return HttpRequest.POST(path, """
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
                {"issuer": "%1$s", "jwks_uri": "%1$s/.well-known/jwks.json", "authorization_endpoint": "%1$s/authorize",
                 "token_endpoint": "%1$s/token", "response_types_supported": ["code"],
                 "subject_types_supported": ["public"], "id_token_signing_alg_values_supported": ["RS256"]}"""
                .formatted(issuer);
        }

        @Get(uri = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON)
        String keys() {
            return jwks;
        }
    }
}
