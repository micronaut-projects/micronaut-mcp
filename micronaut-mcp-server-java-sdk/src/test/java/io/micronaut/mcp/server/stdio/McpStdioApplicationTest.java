package io.micronaut.mcp.server.stdio;

import io.micronaut.context.ApplicationContext;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.runtime.ApplicationConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpStdioApplicationTest {

    @Test
    void testMcpStdioApplicationCanStartAndStopWithoutExceptions() {

        try (ApplicationContext ctx = ApplicationContext.run(Map.of("micronaut.mcp.server.transport", "STDIO"))) {
            var app = new McpStdioApplication(ctx, ctx.getBean(ApplicationConfiguration.class), ctx.getBean(McpServerConfiguration.class));
            assertDoesNotThrow(app::start);
            assertTrue(app.isRunning());
            assertEquals("STDIO sync", app.getDescription());
            assertDoesNotThrow(app::close);
        }
    }
}
