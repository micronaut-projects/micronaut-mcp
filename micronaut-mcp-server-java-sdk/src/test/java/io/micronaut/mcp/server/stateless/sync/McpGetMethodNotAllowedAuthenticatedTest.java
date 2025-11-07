package io.micronaut.mcp.server.stateless.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "spec.name", value = "McpGetMethodNotAllowedAuthenticatedTest")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.security.intercept-url-map[0].access[0]", value = "isAuthenticated()")
@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
@MicronautTest
class McpGetMethodNotAllowedAuthenticatedTest {

    @Test
    void pingSerialization(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.GET("/mcp");
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(req, String.class));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getStatus());
    }

    @Requires(property = "spec.name", value = "McpGetMethodNotAllowedAuthenticatedTest")
    @Singleton
    static class MockAuthenticationFetcher implements AuthenticationFetcher {
        @Override
        public Publisher<Authentication> fetchAuthentication(Object request) {
            return Publishers.just(Authentication.build("sdelamo"));
        }
    }

}
