package io.micronaut.mcp.conf;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest(startApplication = false)
class McpServerConfigurationTest {

    @Test
    void statelessAsync(McpServerConfiguration configuration) {
        assertEquals(Transport.HTTP, configuration.getTransport());
    }

    @Test
    void defaultEndpoint(McpServerConfiguration configuration) {
        assertEquals("/mcp", configuration.getEndpoint());
    }
}
