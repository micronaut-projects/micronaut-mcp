package io.micronaut.mcp.server.sdk.stateless;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.type", value = "STATELESS_ASYNC")
@MicronautTest(startApplication = false)
class StatelessAsyncMcpControllerTest {

    @Inject
    BeanContext beanContext;

    @Test
    void beanMcpControllerExists() {
        assertTrue(beanContext.containsBean(McpController.class));
    }
}
