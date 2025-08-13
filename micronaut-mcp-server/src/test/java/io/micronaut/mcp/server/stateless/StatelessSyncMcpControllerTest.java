package io.micronaut.mcp.server.stateless;

import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class StatelessSyncMcpControllerTest {

    @Inject
    BeanContext beanContext;

    @Test
    void beanMcpControllerExists() {
        assertTrue(beanContext.containsBean(McpController.class));
    }
}
