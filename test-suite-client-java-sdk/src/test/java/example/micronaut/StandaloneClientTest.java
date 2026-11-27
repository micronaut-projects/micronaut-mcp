package example.micronaut;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.propagation.PropagatedContextElement;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.client.javasdk.McpClientTool;
import io.micronaut.mcp.client.javasdk.McpClientTools;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandaloneClientTest {
    @Test
    void configuredClientDiscoversToolsFromTheBlockingExecutor() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String endpoint = "/team/mcp?tenant=a%2Fb";
        var requests = new ConcurrentLinkedQueue<String>();
        JsonMapper mapper = JsonMapper.createDefault();
        server.createContext("/", exchange -> {
            try (exchange) {
                requests.add(exchange.getRequestURI() + " " + exchange.getRequestHeaders().getFirst("Authorization"));
                respond(exchange, mapper);
            }
        });
        server.start();
        try (var context = ApplicationContext.run(Map.of(
            "micronaut.mcp.client.http.remote.url", "http://127.0.0.1:" + server.getAddress().getPort() + endpoint,
            "micronaut.mcp.client.http.remote.headers.Authorization", "Bearer test",
            "micronaut.executors.blocking.type", "fixed",
            "micronaut.executors.blocking.n-threads", "1"))) {
            var tools = context.getBean(McpClientTools.class);
            ExecutorService blocking = context.getBean(ExecutorService.class, Qualifiers.byName(TaskExecutors.BLOCKING));
            var names = blocking.submit(() -> tools.tools().stream().map(McpClientTool::name).toList());
            assertEquals(List.of("weather"), names.get(15, TimeUnit.SECONDS));
            assertFalse(requests.isEmpty());
            assertTrue(requests.stream().allMatch(r -> r.equals(endpoint + " Bearer test")), requests::toString);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void discoveryUsesTheCurrentCatalogLanguageOnEveryCall() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        JsonMapper mapper = JsonMapper.createDefault();
        server.createContext("/mcp", exchange -> {
            try (exchange) {
                respond(exchange, mapper);
            }
        });
        server.start();
        try (var context = ApplicationContext.run(Map.of(
            "spec.name", "catalogLanguage",
            "micronaut.mcp.client.http.remote.url", "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp"))) {
            var all = context.getBean(McpClientTools.class);
            var named = context.getBean(McpClientTools.class, Qualifiers.byName("remote"));
            assertEquals(List.of("tiempo"), toolNames(all, "es"));
            assertEquals(List.of("weather"), toolNames(named, "en"));
            assertEquals(List.of("tiempo"), toolNames(named, "es"));
        } finally {
            server.stop(0);
        }
    }

    private static List<String> toolNames(McpClientTools tools, String language) {
        return PropagatedContext.getOrEmpty().plus(new CatalogLanguage(language))
            .propagate(() -> tools.tools().stream().map(McpClientTool::name).toList());
    }

    private record CatalogLanguage(String value) implements PropagatedContextElement {
    }

    @Singleton
    @Requires(property = "spec.name", value = "catalogLanguage")
    static class CatalogHeaders implements McpClientHeadersProvider {
        @Override
        public Map<String, String> headers(McpClientHttpConfiguration connection) {
            return PropagatedContext.getOrEmpty().find(CatalogLanguage.class)
                .map(language -> Map.of("Accept-Language", language.value())).orElse(Map.of());
        }
    }

    private static void respond(HttpExchange exchange, JsonMapper mapper) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        var message = mapper.readValue(exchange.getRequestBody(), Map.class);
        if (!message.containsKey("id")) {
            exchange.sendResponseHeaders(202, -1);
            return;
        }
        Object result = switch (message.get("method").toString()) {
            case "initialize" -> Map.of("protocolVersion", "2025-11-25", "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of("name", "standalone-test", "version", "1"));
            case "tools/list" -> {
                String language = exchange.getRequestHeaders().getFirst("Accept-Language");
                String name = "es".equals(language) ? "tiempo" : "weather";
                yield Map.of("tools", List.of(Map.of("name", name, "inputSchema", Map.of("type", "object"))));
            }
            default -> throw new AssertionError(message);
        };
        byte[] response = mapper.writeValueAsBytes(Map.of("jsonrpc", "2.0", "id", message.get("id"), "result", result));
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
    }

    @Test
    void standaloneStdioClientInitializes() {
        var command = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("java.class.path"), StdioPeer.class.getName());
        try (var context = ApplicationContext.run(Map.of("micronaut.mcp.client.stdio.local.command", command))) {
            var client = context.getBean(McpSyncClient.class, Qualifiers.byName("local"));
            assertEquals("standalone-stdio", client.getServerInfo().name());
        }
    }

    @Test
    void applicationSpecsProduceLazyQualifiedClientsWithoutConnectionConfiguration() {
        try (var context = ApplicationContext.run()) {
            assertFalse(context.getBean(McpSyncClient.class, Qualifiers.byName("custom")).isInitialized());
            assertFalse(context.getBean(McpAsyncClient.class, Qualifiers.byName("custom")).isInitialized());
        }
    }

    @Factory
    static class Specs {
        @Named("custom")
        @Singleton
        McpClient.SyncSpec sync(McpJsonMapper mapper, JsonSchemaValidator validator) {
            return McpClient.sync(transport(mapper)).jsonSchemaValidator(validator);
        }

        @Named("custom")
        @Singleton
        McpClient.AsyncSpec async(McpJsonMapper mapper, JsonSchemaValidator validator) {
            return McpClient.async(transport(mapper)).jsonSchemaValidator(validator);
        }

        private static HttpClientStreamableHttpTransport transport(McpJsonMapper mapper) {
            return HttpClientStreamableHttpTransport.builder("http://127.0.0.1:1").jsonMapper(mapper).build();
        }
    }
}
