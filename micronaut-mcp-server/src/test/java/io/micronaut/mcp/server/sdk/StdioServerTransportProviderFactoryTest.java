package io.micronaut.mcp.server.sdk;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Property(name = "micronaut.mcp.server.type", value = "SYNC")
@MicronautTest(startApplication = false)
class StdioServerTransportProviderFactoryTest {

    @Inject
    BeanContext beanContext;

    @Test
    void mcpServerTransportProviderBeanExists() {
        assertTrue(beanContext.containsBean(McpServerTransportProvider.class));

    }
}
