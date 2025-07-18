package io.micronaut.mcp.http.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpControllerConfigurationPropertiesTest {

    @Test
    void settersAndGetters() {
        McpControllerConfigurationProperties properties = new McpControllerConfigurationProperties();
        properties.setEnabled(false);
        properties.setPath("/modelcontextprotocol");
        assertFalse(properties.isEnabled());
        assertEquals("/modelcontextprotocol", properties.getPath());
    }
}
