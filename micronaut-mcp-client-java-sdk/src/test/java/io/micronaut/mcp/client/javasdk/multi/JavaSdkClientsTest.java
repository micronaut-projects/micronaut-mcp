package io.micronaut.mcp.client.javasdk.multi;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.client.javasdk.McpClientTool;
import io.micronaut.mcp.client.javasdk.McpClientToolNameMapper;
import io.micronaut.mcp.client.javasdk.McpClientTools;
import io.micronaut.mcp.client.javasdk.McpLoggingHandler;
import io.micronaut.mcp.client.javasdk.McpProgressHandler;
import io.micronaut.mcp.client.javasdk.McpSamplingHandler;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "moon.enabled", value = "false")
@Property(name = "spec.name", value = "JavaSdkClientsTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaSdkClientsTest implements TestPropertyProvider {

    @Inject
    McpClientTools tools;

    @Inject
    @Named("local")
    McpClientTools localTools;

    @Inject
    @Named("local")
    McpSyncClient localClient;

    @Inject
    @Named("embeddedServer")
    McpSyncClient httpClient;

    @Inject
    Notifications notifications;

    @Override
    public Map<String, String> getProperties() {
        String java = ProcessHandle.current().info().command().orElse("java");
        String logback = Path.of("src/test/resources/logback-stderr.xml").toAbsolutePath().toString();
        List<String> command = List.of(java, "-Dlogback.configurationFile=" + logback,
            "-cp", System.getProperty("java.class.path"), StdioServer.class.getName());
        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < command.size(); i++) {
            properties.put("micronaut.mcp.client.stdio.local.command[" + i + "]", command.get(i));
        }
        properties.put("micronaut.mcp.client.stdio.local.request-timeout", "30s");
        return properties;
    }

    @Test
    void theToolsOfEveryConnectionAreProvided() {
        assertEquals(Set.of("embeddedServer__importCatalog", "local__hello", "local__haiku"), names(tools.tools()));
        assertEquals(Set.of("local__hello", "local__haiku"), names(localTools.tools()));
    }

    @Test
    void aProvidedToolCanBeCalled() {
        McpClientTool hello = localTools.tools().stream().filter(t -> t.tool().name().equals("hello")).findFirst().orElseThrow();
        assertEquals("Hello Micronaut", text(hello.call(Map.of("name", "Micronaut"))));
    }

    @Test
    void theSamplingHandlerAnswersTheServer() {
        localClient.initialize();
        assertTrue(localClient.getClientCapabilities().sampling() != null);
        McpSchema.CallToolResult result = localClient.callTool(new McpSchema.CallToolRequest("haiku", Map.of()));
        assertEquals("The model wrote: An old silent pond (from local)", text(result));
    }

    @Test
    void progressAndLogMessagesReachTheHandlers() {
        httpClient.initialize();
        McpSchema.CallToolResult result = httpClient.callTool(new McpSchema.CallToolRequest("importCatalog", Map.of("pages", 2), Map.of("progressToken", "import-1")));
        assertEquals("done", text(result));
        assertEquals(List.of("embeddedServer 1.0/2.0", "embeddedServer 2.0/2.0"), notifications.progress);
        assertEquals(List.of("embeddedServer info imported 2 pages"), notifications.logs);
    }

    private static Set<String> names(List<McpClientTool> tools) {
        return tools.stream().map(McpClientTool::name).collect(Collectors.toSet());
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    @Requires(property = "spec.name", value = "JavaSdkClientsTest")
    @Singleton
    static class HttpTools {
        @Tool(description = "Imports a catalog")
        String importCatalog(int pages, McpRequestContext context) {
            for (int page = 1; page <= pages; page++) {
                context.progress(page, (double) pages, null);
            }
            context.log(McpSchema.LoggingLevel.INFO, "catalog", "imported " + pages + " pages");
            return "done";
        }
    }

    @Requires(property = "spec.name", value = "JavaSdkClientsTest")
    @Singleton
    static class Handlers implements McpClientToolNameMapper, McpSamplingHandler {
        @Override
        public String apply(String client, McpSchema.Tool tool) {
            return client + "__" + tool.name();
        }

        @Override
        public McpSchema.CreateMessageResult sample(String client, McpSchema.CreateMessageRequest request) {
            return McpSchema.CreateMessageResult.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(new McpSchema.TextContent("An old silent pond (from " + client + ")"))
                .model("test")
                .build();
        }
    }

    @Requires(property = "spec.name", value = "JavaSdkClientsTest")
    @Singleton
    static class Notifications implements McpProgressHandler, McpLoggingHandler {
        final List<String> progress = new CopyOnWriteArrayList<>();
        final List<String> logs = new CopyOnWriteArrayList<>();

        @Override
        public void progress(String client, McpSchema.ProgressNotification notification) {
            progress.add(client + " " + notification.progress() + "/" + notification.total());
        }

        @Override
        public void log(String client, McpSchema.LoggingMessageNotification notification) {
            logs.add(client + " " + notification.level().name().toLowerCase() + " " + notification.data());
        }
    }
}
