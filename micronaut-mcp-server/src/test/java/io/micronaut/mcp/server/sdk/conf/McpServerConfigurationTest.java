package io.micronaut.mcp.server.sdk.conf;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Property(name = "micronaut.mcp.server.type", value = "STATELESS_SYNC")
@MicronautTest(startApplication = false)
class McpServerConfigurationTest {

    @Test
    void statelessAsync(McpServerConfiguration configuration) {
        assertEquals(ServerType.STATELESS_SYNC, configuration.getType());
    }

    @Test
    void defaultEndpoint(McpServerConfiguration configuration) {
        assertEquals("/mcp", configuration.getEndpoint());
    }
}
