package io.micronaut.mcp.server;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
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
@Property(name = "micronaut.mcp.server.info.name", value = "javaone-mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.info.title", value = "JavaOne")
@Property(name = "micronaut.mcp.server.info.icons[0].src", value = "https://example.com/javaone.png")
@MicronautTest(startApplication = false)
class McpSyncServerTest {
    @Inject
    BeanContext beanContext;

    @Test
    void defaultMcpServerSync() {
        assertTrue(beanContext.containsBean(McpSyncServer.class));
        assertFalse(beanContext.containsBean(McpAsyncServer.class));
    }

    @Test
    void serverInfo() {
        McpSchema.Implementation serverInfo = beanContext.getBean(McpSyncServer.class).getServerInfo();
        assertEquals("JavaOne", serverInfo.title());
        assertEquals(List.of(new McpSchema.Icon("https://example.com/javaone.png", null, null, null)), serverInfo.icons());
    }
}
