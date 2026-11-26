package io.micronaut.mcp.client.langchain4j.multi;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import io.micronaut.json.JsonMapper;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.runtime.server.EmbeddedServer;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helpers shared by the tests that connect to several MCP servers.
 */
final class MultiClientTestSupport {
    private MultiClientTestSupport() {
    }

    /**
     * @param provider The tool provider
     * @return The names of the tools the provider provides
     */
    static Set<String> toolNames(McpToolProvider provider) {
        return provider.provideTools(new ToolProviderRequest("memory", UserMessage.from("hi"))).tools().keySet().stream()
            .map(ToolSpecification::name)
            .collect(Collectors.toSet());
    }

    /**
     * @param healthIndicators The health indicators of the application
     * @return The result of the {@code mcp} health indicator
     */
    static HealthResult mcpHealth(List<HealthIndicator> healthIndicators) {
        return healthIndicators.stream()
            .map(indicator -> Flux.from(indicator.getResult()).blockFirst())
            .filter(result -> result != null && result.getName().equals("mcp"))
            .findFirst().orElseThrow();
    }

    /**
     * @param server The embedded server
     * @return The response of the health endpoint
     * @throws IOException When the request fails
     * @throws InterruptedException When the request is interrupted
     */
    static HttpResponse<String> health(EmbeddedServer server) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(server.getURI().resolve("/health")).build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    /**
     * @param jsonMapper The JSON mapper
     * @param response The response of the health endpoint
     * @return The details of the {@code mcp} health indicator, by connection
     * @throws IOException When the response cannot be read
     */
    static Map<?, ?> mcpHealthDetails(JsonMapper jsonMapper, HttpResponse<String> response) throws IOException {
        Map<?, ?> body = jsonMapper.readValue(response.body(), Map.class);
        Map<?, ?> mcp = (Map<?, ?>) ((Map<?, ?>) body.get("details")).get("mcp");
        return (Map<?, ?>) mcp.get("details");
    }

    /**
     * @param connection The name of the STDIO connection
     * @param main The class whose main method starts the server
     * @param jvmArguments The arguments of the server JVM
     * @return The properties of a STDIO connection that starts the server as a child JVM with the test class path
     */
    static Map<String, String> stdioConnection(String connection, Class<?> main, String... jvmArguments) {
        List<String> command = new ArrayList<>();
        command.add(ProcessHandle.current().info().command().orElse("java"));
        command.addAll(List.of(jvmArguments));
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), main.getName()));
        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < command.size(); i++) {
            properties.put("micronaut.mcp.client.stdio." + connection + ".command[" + i + "]", command.get(i));
        }
        return properties;
    }
}
