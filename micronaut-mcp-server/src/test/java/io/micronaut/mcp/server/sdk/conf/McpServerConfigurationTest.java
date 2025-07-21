package io.micronaut.mcp.server.sdk.conf;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class McpServerConfigurationTest {

    @Test
    void defaultToAysnc(McpServerConfiguration configuration) {
        assertFalse(configuration.isAsync());
    }
}
