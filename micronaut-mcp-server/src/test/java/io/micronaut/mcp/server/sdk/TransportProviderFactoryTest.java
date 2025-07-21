package io.micronaut.mcp.server.sdk;

import io.micronaut.context.BeanContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class TransportProviderFactoryTest {

    @Inject
    BeanContext beanContext;

    @Test
    void stdioTransportHasNameQualifier() {
        assertTrue(beanContext.containsBean(McpServerTransportProvider.class));
        assertTrue(beanContext.containsBean(McpServerTransportProvider.class, Qualifiers.byName("stdio")));

    }
}
