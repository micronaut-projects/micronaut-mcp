package io.micronaut.mcp.client.langchain4j.multi;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.health.HealthStatus;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.mcp.client.langchain4j.McpToolNameMapper;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.mcpHealth;
import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.toolNames;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "moon.enabled", value = "true")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.info.name", value = "moon")
@Property(name = "micronaut.mcp.server.info.version", value = "1")
class McpClientIntegrationTest {
    @Inject
    EmbeddedServer server;

    @Inject
    McpServerConfiguration serverConfiguration;

    @Inject
    @Named("embeddedServer")
    McpClient applicationClient;

    @Test
    void primaryProviderIncludesApplicationClientsAlongsideConfiguredConnections() {
        try (var context = ApplicationContext.run(Map.of("micronaut.mcp.client.http.configured.url", url()))) {
            context.registerSingleton(McpClient.class, applicationClient, Qualifiers.byName("external"));
            context.registerSingleton(McpToolNameMapper.class, (client, tool) -> client.key() + "_" + tool.name());
            assertEquals(Set.of("embeddedServer_current-moon-phase", "embeddedServer_moon-phase-at-date",
                    "configured_current-moon-phase", "configured_moon-phase-at-date"),
                toolNames(context.getBean(McpToolProvider.class)));
        }
    }

    @Test
    void applicationTransportSuppliesItsNamedBuilderAndClient() {
        try (var context = ApplicationContext.run(Map.of("micronaut.mcp.client.http.configured.url", url()))) {
            McpTransport transport = new StreamableHttpMcpTransport.Builder().url(url()).build();
            context.registerSingleton(McpTransport.class, transport, Qualifiers.byName("custom"));
            context.registerSingleton(McpToolNameMapper.class, (client, tool) -> client.key() + "_" + tool.name());
            assertTrue(context.containsBean(DefaultMcpClient.Builder.class, Qualifiers.byName("custom")));
            McpClient customClient = context.getBean(McpClient.class, Qualifiers.byName("custom"));
            assertTrue(customClient.listTools().stream().anyMatch(tool -> tool.name().equals("current-moon-phase")));
            assertEquals(Set.of(customClient.key() + "_current-moon-phase", customClient.key() + "_moon-phase-at-date",
                    "configured_current-moon-phase", "configured_moon-phase-at-date"),
                toolNames(context.getBean(McpToolProvider.class)));
        }
    }

    @Test
    void healthyServerIsUpWithOneBlockingWorker() {
        try (var context = ApplicationContext.run(Map.of(
            "micronaut.mcp.client.http.remote.url", url(),
            "micronaut.executors.blocking.type", "fixed",
            "micronaut.executors.blocking.number-of-threads", 1,
            "endpoints.health.mcp.timeout", "2s"))) {
            var health = mcpHealth(List.copyOf(context.getBeansOfType(HealthIndicator.class)));
            assertEquals(HealthStatus.UP, health.getStatus());
            assertEquals(Map.of("remote", Map.of("status", "UP")), health.getDetails());
        }
    }

    private String url() {
        return server.getURI().resolve(serverConfiguration.getEndpoint()).toString();
    }
}
