package io.modelcontextprotocol.server.http.tck.async;

import io.micronaut.context.ApplicationContext;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.micronaut.mcp.server.stateless.transport.McpHttpServer;
import io.micronaut.mcp.server.stateless.transport.McpHttpServerSupplier;
import io.micronaut.runtime.server.EmbeddedServer;

import java.util.Map;

public class MicronautSyncMcpHttpServerSupplier implements McpHttpServerSupplier {
    @Override
    public McpHttpServer get() {
        Map<String, Object> configuration = Map.of(
        "micronaut.mcp.server.transport", "HTTP",
        "micronaut.mcp.server.info.name", "mcp-server",
        "micronaut.mcp.server.info.version", "0.0.1"
        );
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, configuration);
        McpServerConfiguration mcpServerConfiguration = server.getApplicationContext().getBean(McpServerConfiguration.class);
        return new McpHttpServer() {

            @Override
            public void start() {

            }

            @Override
            public int getPort() {
                return server.getPort();
            }

            @Override
            public String getEndpoint() {
                return mcpServerConfiguration.getEndpoint();
            }

            @Override
            public void close() throws Exception {
                server.close();
            }
        };
    }
}
