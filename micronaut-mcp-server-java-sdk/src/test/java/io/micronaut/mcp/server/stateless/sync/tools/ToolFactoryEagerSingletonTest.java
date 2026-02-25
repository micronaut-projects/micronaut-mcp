package io.micronaut.mcp.server.stateless.sync.tools;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolFactoryEagerSingletonTest {

    @Test
    void toolAnnotationsTestWithEagerInitialization() {
        Map<String, Object> config = Map.of("micronaut.mcp.server.info.name", "mcp-server",
        "micronaut.mcp.server.info.version", "0.0.1",
        "micronaut.mcp.server.transport", "HTTP",
        "spec.name", "ToolFactoryEagerSingletonTest");
        EmbeddedServer server = assertDoesNotThrow(() ->
            ApplicationContext.builder(config)
                .eagerInitSingletons(true)
                .run(EmbeddedServer.class)
        );
        HttpClient httpClient = server.getApplicationContext().createBean(HttpClient.class, server.getURL());
        BlockingHttpClient client = httpClient.toBlocking();
        String json = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)));
        assertTrue(json.contains(",\"annotations\":{\"title\":\"Hello World\",\"readOnlyHint\":true,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":false,\"returnDirect\":true}}]}}"), json);
        client.close();
        httpClient.close();
        server.close();
    }

    @Requires(property = "spec.name", value = "ToolFactoryEagerSingletonTest")
    @Factory
    static class HelloWorldTool {
        @Singleton
        McpStatelessServerFeatures.SyncToolSpecification helloWorldTool() {
            return McpStatelessServerFeatures.SyncToolSpecification.builder().tool(McpSchema.Tool.builder()
                    .name("helloWorld")
                    .title("Hello World")
                    .annotations(new McpSchema.ToolAnnotations("Hello World", true, false, true, false, true))
                .build()).callHandler((mcpTransportContext, callToolRequest) -> McpSchema.CallToolResult.builder().addTextContent("Hello, World!").isError(false).build()).build();
        }
    }
}
