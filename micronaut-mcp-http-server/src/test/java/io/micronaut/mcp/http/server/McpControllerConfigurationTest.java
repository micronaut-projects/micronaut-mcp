package io.micronaut.mcp.http.server;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class McpControllerConfigurationTest {

    @Test
    void defaultValuesForMcpControllerConfiguration(McpControllerConfiguration config) {
        assertTrue(config.isEnabled());
        assertEquals("/mcp", config.getPath());

    }
}
