package io.micronaut.mcp.server;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
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
