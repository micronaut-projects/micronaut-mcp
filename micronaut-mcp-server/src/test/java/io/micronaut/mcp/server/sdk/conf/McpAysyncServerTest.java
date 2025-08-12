package io.micronaut.mcp.server.sdk.conf;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.type", value = "ASYNC")
@Property(name = "micronaut.mcp.server.info.name", value = "javaone-mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@MicronautTest(startApplication = false)
class McpAysyncServerTest {
    @Inject
    BeanContext beanContext;

    @Test
    void defaultMcpServerSync() {
        assertFalse(beanContext.containsBean(McpSyncServer.class));
        assertTrue(beanContext.containsBean(McpAsyncServer.class));
    }
}
