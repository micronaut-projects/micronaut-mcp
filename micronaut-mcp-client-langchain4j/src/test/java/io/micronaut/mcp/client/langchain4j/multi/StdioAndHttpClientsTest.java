package io.micronaut.mcp.client.langchain4j.multi;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.client.langchain4j.McpToolNameMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.mcpHealth;
import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.stdioConnection;
import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.toolNames;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "StdioAndHttpClientsTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StdioAndHttpClientsTest implements TestPropertyProvider {

    @Inject
    List<McpClient> clients;

    @Inject
    @Named("local")
    McpClient localClient;

    @Inject
    McpToolProvider toolProvider;

    @Inject
    @Named("local")
    McpToolProvider localToolProvider;

    @Inject
    List<HealthIndicator> healthIndicators;

    @Override
    public Map<String, String> getProperties() {
        // The server logs to the standard error, as its standard output carries the protocol
        Map<String, String> properties = new HashMap<>(stdioConnection("local", StdioServer.class,
            "-Dlogback.configurationFile=logback-stderr.xml"));
        properties.put("micronaut.mcp.client.stdio.local.request-timeout", "20s");
        return properties;
    }

    @Test
    void aClientIsCreatedForEachConnectionKeyedByItsName() {
        assertEquals(Set.of("embeddedServer", "local"), clients.stream().map(McpClient::key).collect(Collectors.toSet()));
    }

    @Test
    void thePrimaryToolProviderProvidesTheToolsOfEveryClient() {
        assertEquals(Set.of("embeddedServer__forecast", "local__hello"), toolNames(toolProvider));
    }

    @Test
    void eachClientHasItsOwnToolProvider() {
        assertEquals(Set.of("local__hello"), toolNames(localToolProvider));
    }

    @Test
    void toolsOfTheStdioServerCanBeCalled() {
        String result = localClient.executeTool(ToolExecutionRequest.builder()
            .name("hello").arguments("{\"name\": \"Micronaut\"}").build()).resultText();
        assertEquals("Hello Micronaut from STDIO", result);
    }

    @Test
    void theHealthIndicatorPingsEveryServer() {
        HealthResult mcp = mcpHealth(healthIndicators);
        assertEquals(HealthStatus.UP, mcp.getStatus());
        assertEquals(Map.of("embeddedServer", "UP", "local", "UP"), mcp.getDetails());
    }

    @Requires(property = "spec.name", value = "StdioAndHttpClientsTest")
    @Singleton
    static class HttpTools {
        @Tool(description = "Forecasts the weather")
        String forecast(String city) {
            return "sunny";
        }
    }

    @Requires(property = "spec.name", value = "StdioAndHttpClientsTest")
    @Singleton
    static class PrefixingNameMapper implements McpToolNameMapper {
        @Override
        public String apply(McpClient client, ToolSpecification tool) {
            return client.key() + "__" + tool.name();
        }
    }
}
