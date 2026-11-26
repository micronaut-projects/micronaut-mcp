package io.micronaut.mcp.server;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.info.name", value = "javaone-mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.info.title", value = "JavaOne")
@Property(name = "micronaut.mcp.server.info.icons[0].src", value = "https://example.com/javaone.png")
@MicronautTest(startApplication = false)
class McpAysyncServerTest {
    @Inject
    BeanContext beanContext;

    @Test
    void defaultMcpServerSync() {
        assertFalse(beanContext.containsBean(McpSyncServer.class));
        assertTrue(beanContext.containsBean(McpAsyncServer.class));
    }

    @Test
    void serverInfo() {
        McpSchema.Implementation serverInfo = beanContext.getBean(McpAsyncServer.class).getServerInfo();
        assertEquals("JavaOne", serverInfo.title());
        assertEquals(List.of(new McpSchema.Icon("https://example.com/javaone.png", null, null, null)), serverInfo.icons());
    }
}
