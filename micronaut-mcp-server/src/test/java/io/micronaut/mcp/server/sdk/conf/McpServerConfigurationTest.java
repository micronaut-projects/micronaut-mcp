package io.micronaut.mcp.server.sdk.conf;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class McpServerConfigurationTest {

    @Test
    void defaultToStatelessAsync(McpServerConfiguration configuration) {
        assertEquals(ServerType.STATELESS_SYNC, configuration.getType());
    }

    @Test
    void defaultEndpoint(McpServerConfiguration configuration) {
        assertEquals("/mcp", configuration.getEndpoint());
    }
}
