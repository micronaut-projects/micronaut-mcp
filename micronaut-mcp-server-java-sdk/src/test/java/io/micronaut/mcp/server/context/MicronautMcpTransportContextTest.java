package io.micronaut.mcp.server.context;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautMcpTransportContextTest")
@MicronautTest
class MicronautMcpTransportContextTest {

    @Requires(property = "spec.name", value = "MicronautMcpTransportContextTest")
    @Singleton
    static class TestAuthenticationFetcher implements AuthenticationFetcher<HttpRequest<?>> {
        @Override
        public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
            return Publishers.just(Authentication.build("sdelamo"));
        }
    }
}
