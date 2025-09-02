package io.micronaut.mcp.conf;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@MicronautTest(startApplication = false)
class ResourcesConfigurationTest {

    @Test
    void resourcesConfiguration(ResourcesConfiguration resourcesConfiguration) {
        assertFalse(resourcesConfiguration.isListChanged());
        assertFalse(resourcesConfiguration.isSubscribe());
    }
}
