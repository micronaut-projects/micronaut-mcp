package io.micronaut.mcp.server.sdk.conf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpServerConfigurationPropertiesTest {

    @Test
    void canSetAsyncViaSetter() {
        McpServerConfigurationProperties properties = new McpServerConfigurationProperties();
        properties.setAsync(true);
        assertTrue(properties.isAsync());
    }
}
