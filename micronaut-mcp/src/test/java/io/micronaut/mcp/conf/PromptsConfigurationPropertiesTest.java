package io.micronaut.mcp.conf;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.prompts.list-changed", value = StringUtils.TRUE)
@MicronautTest(startApplication = false)
class PromptsConfigurationPropertiesTest {
    @Test
    void promptsConfiguration(PromptsConfiguration promptsConfiguration) {
        assertTrue(promptsConfiguration.isListChanged());
    }
}
