package io.micronaut.mcp.server.conf;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.tools.list-changed", value = StringUtils.TRUE)
@MicronautTest(startApplication = false)
class ToolsConfigurationPropertiesTest {
    @Test
    void toolsConfiguration(ToolsConfiguration toolsConfiguration) {
        assertTrue(toolsConfiguration.isListChanged());
    }
}
