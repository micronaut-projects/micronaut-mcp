package io.micronaut.mcp.server.sdk.stateless;

import io.micronaut.context.BeanContext;
import io.micronaut.mcp.server.sdk.stateless.MicronautMcpStatelessServerTransport;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class MicronautMcpStatelessServerTransportTest {
    @Inject
    BeanContext beanContext;

    @Test
    void beanOfTypeMicronautMcpStatelessServerTransportExists() {
        assertTrue(beanContext.containsBean(MicronautMcpStatelessServerTransport.class));
    }
}
