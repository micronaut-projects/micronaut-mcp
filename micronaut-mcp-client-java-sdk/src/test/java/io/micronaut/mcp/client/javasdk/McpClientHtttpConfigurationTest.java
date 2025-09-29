package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Property(name = "micronaut.mcp.client.http.servera.url", value = "http://micronaut.io/")
@Property(name = "micronaut.mcp.client.http.servera.request-timeout", value = "5s")
@MicronautTest(startApplication = false)
class McpClientHtttpConfigurationTest {
    @Test
    void populateViaConfig(McpClientHtttpConfiguration configuration) {
        assertEquals("servera", configuration.getName());
        assertEquals(URI.create("http://micronaut.io/"),  configuration.getUrl());
        assertEquals(Duration.ofSeconds(5), configuration.getRequestTimeout());
    }
}
