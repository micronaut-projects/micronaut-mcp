package io.micronaut.mcp.server.sdk;

import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class ServerCapabilitiesTest {

    @Inject
    BeanContext beanContext;

    @Test
    void beanOfTypeServerCapabilitiesExists() {
        assertTrue(beanContext.containsBean(McpSchema.ServerCapabilities.class));
    }
}
