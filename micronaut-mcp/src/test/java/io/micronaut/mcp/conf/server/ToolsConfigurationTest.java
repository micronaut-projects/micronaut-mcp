package io.micronaut.mcp.conf.server;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@MicronautTest(startApplication = false)
class ToolsConfigurationTest {

    @Test
    void toolsConfiguration(ToolsConfiguration toolsConfiguration) {
        assertFalse(toolsConfiguration.isListChanged());
    }
}
