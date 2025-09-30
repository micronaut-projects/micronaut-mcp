package io.micronaut.mcp.conf.server;

import io.micronaut.mcp.conf.Transport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerConfigurationPropertiesTest {

    @Test
    void canSetTransportViaSetter() {
        McpServerConfigurationProperties properties = new McpServerConfigurationProperties();
        properties.setTransport(Transport.STDIO);
        assertEquals(Transport.STDIO, properties.getTransport());
    }

    @Test
    void canSetReactiveViaSetter() {
        McpServerConfigurationProperties properties = new McpServerConfigurationProperties();
        properties.setReactive(true);
        assertTrue(properties.isReactive());
    }

    @Test
    void canSetEndpointViaSetter() {
        McpServerConfigurationProperties properties = new McpServerConfigurationProperties();
        properties.setEndpoint("/modelcontextprotocol");
        assertEquals("/modelcontextprotocol", properties.getEndpoint());
    }
}
