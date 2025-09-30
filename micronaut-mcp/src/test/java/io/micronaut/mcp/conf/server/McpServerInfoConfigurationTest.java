package io.micronaut.mcp.conf.server;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "javaone-mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@MicronautTest(startApplication = false)
class McpServerInfoConfigurationTest {
    @Test
    void serverInfoCanBePopulatedViaConfig(McpServerInfoConfiguration config) {
        assertEquals("javaone-mcp-server", config.getName());
        assertEquals("1.0.0", config.getVersion());
    }
}
