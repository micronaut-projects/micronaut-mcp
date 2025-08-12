package io.micronaut.mcp.server.sdk.conf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpServerConfigurationPropertiesTest {

    @Test
    void canSetTypeViaSetter() {
        McpServerConfigurationProperties properties = new McpServerConfigurationProperties();
        properties.setType(ServerType.SYNC);
        assertEquals(ServerType.SYNC, properties.getType());
    }
}
