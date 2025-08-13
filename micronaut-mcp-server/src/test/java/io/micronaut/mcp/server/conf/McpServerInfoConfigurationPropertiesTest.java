package io.micronaut.mcp.server.conf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpServerInfoConfigurationPropertiesTest {

    @Test
    void youCanSetNameAndVersionViaSetters() {
        McpServerInfoConfigurationProperties properties = new McpServerInfoConfigurationProperties();
        properties.setName("javaone-mcp-server");
        properties.setVersion("1.0.0");
        assertEquals("javaone-mcp-server", properties.getName());
        assertEquals("1.0.0", properties.getVersion());
    }
}
