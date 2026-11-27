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
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.security.token.Claims;
import io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private static final String METADATA_PATH = "/.well-known/oauth-protected-resource";
    private static final String PUBLIC_HOST = "mcp.example.com";
    private static final String CONTEXT_PATH = "/ctx";
    private static final String CONTEXT_ENDPOINT = CONTEXT_PATH + "/mcp";

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
            "micronaut.security.token.jwt.claims-validators.issuer", issuer,
            "micronaut.security.token.jwt.claims-validators.not-before", StringUtils.TRUE,
            // only the resolved host the server answers to names the resource in the challenge and the metadata
            "micronaut.server.host-resolution.allowed-hosts[0]", "http://localhost:" + port,
            "micronaut.security.token.jwt.signatures.jwks.idp.url", issuer + "/.well-known/jwks.json",
            "micronaut.security.oauth2.clients.idp.openid.issuer", issuer);
    }

    @AfterAll
    void stopIdp() {
        idp.close();
    }

    @Test
    void aClientCanFollowTheChallengeToDiscoverTheAudienceAndAuthorizationServer() throws JSONException {
        URI metadataUri = metadataUri(rejected(whoami("/mcp")));
        assertEquals(URI.create("http://localhost:" + port + METADATA_PATH + "/mcp"), metadataUri);
        JSONObject metadata = new JSONObject(httpClient.toBlocking().retrieve(metadataUri.getRawPath()));
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
    void aTokenOfAnotherIssuerIsRejected() throws JOSEException {
        String token = token(claims().issuer("https://evil.example").build(), key);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
    }

    @Test
    void aTokenWithoutAnIssuerIsRejected() throws JOSEException {
        String token = token(claims().issuer(null).build(), key);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
    }

    @Test
    void aTokenNotValidYetIsRejected() throws JOSEException {
        String token = token(claims().notBeforeTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS))).build(), key);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
    }

    @Test
    void aTokenWithoutAnExpirationIsRejected() throws JOSEException {
        String token = token(claims().expirationTime(null).build(), key);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected(whoami("/mcp").bearerAuth(token)).getStatus());
    }

    @Test
    void aForgedHostDoesNotNameTheResource() {
        HttpClientResponseException ex = rejected(whoami("/mcp").header("Host", "evil.example"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        String challenge = ex.getResponse().getHeaders().get("WWW-Authenticate");
        assertFalse(challenge.contains("evil.example"), challenge);
        String metadata = httpClient.toBlocking().retrieve(HttpRequest.GET("/.well-known/oauth-protected-resource/mcp")
            .header("X-Forwarded-Host", "evil.example"));
        assertFalse(metadata.contains("evil.example"), metadata);
    }

    @Test
    void behindAProxyTheConfiguredForwardedHeadersNameTheResource() throws JOSEException, JSONException {
        String issuer = issuer();
        Map<String, Object> properties = new HashMap<>(Map.of(
            "micronaut.mcp.server.info.name", "secured",
            "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.transport", "HTTP",
            "micronaut.security.intercept-url-map[0].pattern", "/mcp",
            "micronaut.security.intercept-url-map[0].access[0]", "isAuthenticated()",
            "micronaut.security.token.jwt.signatures.jwks.idp.url", issuer + "/.well-known/jwks.json",
            "micronaut.security.oauth2.clients.idp.openid.issuer", issuer,
            "spec.name", "SecuredMcpServerTest"));
        properties.putAll(Map.of(
            "micronaut.security.token.jwt.claims-validators.audience", "https://mcp.example.com/mcp",
            "micronaut.security.token.jwt.claims-validators.issuer", issuer,
            "micronaut.security.token.jwt.claims-validators.not-before", StringUtils.TRUE,
            // the proxy terminates TLS and sets these headers, replacing any the client sent
            "micronaut.server.host-resolution.protocol-header", "X-Forwarded-Proto",
            "micronaut.server.host-resolution.host-header", "X-Forwarded-Host",
            "micronaut.server.host-resolution.allowed-hosts[0]", "https://mcp[.]example[.]com"));
        try (EmbeddedServer proxied = ApplicationContext.run(EmbeddedServer.class, properties);
             HttpClient client = proxied.getApplicationContext().createBean(HttpClient.class, proxied.getURL())) {
            BlockingHttpClient blocking = client.toBlocking();
            MutableHttpRequest<String> request = proxied(whoami("/mcp"));
            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> blocking.exchange(request, String.class));
            assertEquals("Bearer resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource/mcp\"",
                ex.getResponse().getHeaders().get("WWW-Authenticate"));
            JSONObject metadata = new JSONObject(blocking.retrieve(proxied(HttpRequest.GET("/.well-known/oauth-protected-resource/mcp"))));
            assertEquals("https://mcp.example.com/mcp", metadata.getString("resource"));
            String token = token(claims().audience("https://mcp.example.com/mcp").build(), key);
            assertEquals(HttpStatus.OK, blocking.exchange(proxied(whoami("/mcp")).bearerAuth(token), String.class).getStatus());
            // Once the headers are configured, only they are read: a forged Host or Forwarded header names no resource
            for (MutableHttpRequest<String> forged : List.of(
                    whoami("/mcp").header("Host", "evil.example"),
                    whoami("/mcp").header("Forwarded", "proto=https;host=evil.example"),
                    whoami("/mcp").header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "evil.example"))) {
                HttpClientResponseException rejected = assertThrows(HttpClientResponseException.class, () -> blocking.exchange(forged, String.class));
                assertEquals("Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource/mcp\"",
                    rejected.getResponse().getHeaders().get("WWW-Authenticate"));
            }
        }
    }

    private static <T> MutableHttpRequest<T> proxied(MutableHttpRequest<T> request) {
        return request.header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "mcp.example.com");
    }

    @Test
    void aProxyCanPublishCanonicalDiscoveryForAnApplicationWithAContextPath() throws Exception {
        String browserOrigin = "https://app.example.com";
        String resource = "https://" + PUBLIC_HOST + CONTEXT_ENDPOINT;
        Map<String, Object> properties = new HashMap<>(getProperties());
        properties.putAll(Map.ofEntries(
            Map.entry("spec.name", "SecuredMcpServerTest"),
            Map.entry("micronaut.server.port", -1),
            Map.entry("micronaut.server.context-path", CONTEXT_PATH),
            Map.entry("micronaut.mcp.server.transport", "HTTP"),
            Map.entry("micronaut.mcp.server.info.name", "secured"),
            Map.entry("micronaut.mcp.server.info.version", "1.0.0"),
            Map.entry("micronaut.security.intercept-url-map[0].pattern", "/mcp"),
            Map.entry("micronaut.security.intercept-url-map[0].access[0]", "isAuthenticated()"),
            Map.entry("micronaut.security.token.jwt.claims-validators.audience", resource),
            Map.entry("micronaut.server.host-resolution.protocol-header", "X-Forwarded-Proto"),
            Map.entry("micronaut.server.host-resolution.host-header", "X-Forwarded-Host"),
            Map.entry("micronaut.server.host-resolution.allowed-hosts[0]", "https://mcp[.]example[.]com"),
            Map.entry("micronaut.mcp.server.transport-security.allowed-origins", List.of(browserOrigin)),
            Map.entry("micronaut.server.cors.enabled", true),
            Map.entry("micronaut.server.cors.configurations.web.allowed-origins", List.of(browserOrigin)),
            Map.entry("micronaut.server.cors.configurations.web.exposed-headers", List.of(HttpHeaders.WWW_AUTHENTICATE))));
        HttpClientConfiguration clientConfiguration = new DefaultHttpClientConfiguration();
        clientConfiguration.setFollowRedirects(false);
        try (EmbeddedServer application = ApplicationContext.run(EmbeddedServer.class, properties);
             EmbeddedServer gateway = ApplicationContext.run(EmbeddedServer.class, Map.of(
                 "spec.name", "SecuredMcpServerTestGateway",
                 "micronaut.security.enabled", false,
                 "micronaut.mcp.server.enabled", false,
                 "test.gateway.upstream", application.getURL().toString()));
             HttpClient client = HttpClient.create(gateway.getURL(), clientConfiguration)) {
            BlockingHttpClient blocking = client.toBlocking();
            HttpResponse<?> redirect = blocking.exchange(whoami(CONTEXT_ENDPOINT + "/"));
            assertEquals(HttpStatus.PERMANENT_REDIRECT, redirect.getStatus());
            assertEquals(CONTEXT_ENDPOINT, redirect.getHeaders().get(HttpHeaders.LOCATION));

            HttpClientResponseException challenge = assertThrows(HttpClientResponseException.class, () -> blocking.exchange(
                whoami(redirect.getHeaders().get(HttpHeaders.LOCATION)).header(HttpHeaders.ORIGIN, browserOrigin), String.class));
            assertEquals(HttpHeaders.WWW_AUTHENTICATE, challenge.getResponse().getHeaders().get(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
            URI metadataUri = metadataUri(challenge);
            assertEquals(URI.create("https://" + PUBLIC_HOST + METADATA_PATH + CONTEXT_ENDPOINT), metadataUri);

            HttpResponse<String> metadata = blocking.exchange(HttpRequest.GET(metadataUri.getRawPath())
                .header(HttpHeaders.ORIGIN, browserOrigin), String.class);
            assertEquals(browserOrigin, metadata.getHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
            String audience = new JSONObject(metadata.body()).getString("resource");
            assertEquals(resource, audience);
            String token = token(claims().audience(audience).build(), key);
            assertEquals(HttpStatus.OK, blocking.exchange(whoami(CONTEXT_ENDPOINT).bearerAuth(token), String.class).getStatus());
        }
    }

    private static URI metadataUri(HttpClientResponseException response) {
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
        String challenge = response.getResponse().getHeaders().get(HttpHeaders.WWW_AUTHENTICATE);
        assertNotNull(challenge);
        String prefix = "Bearer resource_metadata=\"";
        assertTrue(challenge.startsWith(prefix) && challenge.endsWith("\""), challenge);
        return URI.create(challenge.substring(prefix.length(), challenge.length() - 1));
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
     * Micronaut Security accepts a token without an expiration; the guide recommends a validator like this one.
     *
     * @param <T> The request type
     */
    @Requires(property = "spec.name", value = "SecuredMcpServerTest")
    @Singleton
    static class ExpirationRequiredClaimsValidator<T> implements GenericJwtClaimsValidator<T> {
        @Override
        public boolean validate(Claims claims, @Nullable T request) {
            return claims.contains(Claims.EXPIRATION_TIME);
        }
    }

    /**
     * Implements the proxy mappings from the Authorization section of the guide.
     */
    @Requires(property = "spec.name", value = "SecuredMcpServerTestGateway")
    @Filter("/**")
    static class Gateway implements HttpServerFilter {
        private final ProxyHttpClient client;
        private final URI upstream;

        Gateway(ProxyHttpClient client, @Value("${test.gateway.upstream}") URI upstream) {
            this.client = client;
            this.upstream = upstream;
        }

        @Override
        public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
            String path = request.getPath();
            if (path.equals(CONTEXT_ENDPOINT + "/")) {
                return Publishers.just(HttpResponse.permanentRedirect(URI.create(CONTEXT_ENDPOINT)));
            }
            if (path.equals(METADATA_PATH + CONTEXT_ENDPOINT)) {
                path = CONTEXT_PATH + path;
            } else if (!path.equals(CONTEXT_ENDPOINT)) {
                return chain.proceed(request);
            }
            MutableHttpRequest<?> forwarded = request.mutate().uri(upstream.resolve(path));
            forwarded.getHeaders().set("X-Forwarded-Proto", "https");
            forwarded.getHeaders().set("X-Forwarded-Host", PUBLIC_HOST);
            return client.proxy(forwarded);
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
