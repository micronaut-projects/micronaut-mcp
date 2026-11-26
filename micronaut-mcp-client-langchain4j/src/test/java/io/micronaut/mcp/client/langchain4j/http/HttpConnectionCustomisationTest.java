package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.service.tool.ToolProviderRequest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.client.langchain4j.McpToolFilter;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the settings and beans that customise the clients and the tool providers of HTTP connections.
 */
class HttpConnectionCustomisationTest {
    private static final String SERVER = "HttpConnectionCustomisationTest-server";
    private static final String CLIENT = "HttpConnectionCustomisationTest-client";

    @Test
    void headersFiltersListenersAndBuilderListenersAreApplied() {
        try (EmbeddedServer mcpServer = ApplicationContext.run(EmbeddedServer.class, Map.of(
            "spec.name", SERVER,
            "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.info.name", "customisation",
            "micronaut.mcp.server.transport", "HTTP"))) {
            String url = mcpServer.getURL().toString() + mcpServer.getApplicationContext().getBean(McpServerConfiguration.class).getEndpoint();
            try (ApplicationContext client = ApplicationContext.run(Map.of(
                "spec.name", CLIENT,
                "micronaut.mcp.client.http.remote.url", url,
                "micronaut.mcp.client.http.remote.headers.X-Api-Key", "secret"))) {
                McpToolProvider primary = client.getBean(McpToolProvider.class);
                McpToolProvider remote = client.getBean(McpToolProvider.class, Qualifiers.byName("remote"));

                assertEquals(Set.of("listened_forecast"), toolNames(primary), "The filter and the builder listener apply to the primary provider");
                assertEquals(Set.of("listened_forecast"), toolNames(remote), "The filter and the builder listener apply to the provider of the connection");
                assertTrue(mcpServer.getApplicationContext().getBean(ApiKeyRecorder.class).keys.contains("secret"), "The header of the connection is sent");
                assertTrue(client.getBean(ToolsListCounter.class).count.get() > 0, "The listener is registered with the client");
            }
        }
    }

    private static Set<String> toolNames(McpToolProvider provider) {
        return provider.provideTools(new ToolProviderRequest("memory", UserMessage.from("hi"))).tools().keySet().stream()
            .map(ToolSpecification::name)
            .collect(Collectors.toSet());
    }

    @Requires(property = "spec.name", value = SERVER)
    @Singleton
    static class ServerTools {
        @Tool(description = "Forecasts the weather")
        String forecast(String city) {
            return "sunny";
        }

        @Tool(description = "A tool that the filter hides")
        String hidden() {
            return "hidden";
        }
    }

    @Requires(property = "spec.name", value = SERVER)
    @ServerFilter("/mcp/**")
    static class ApiKeyRecorder {
        final List<String> keys = new CopyOnWriteArrayList<>();

        @RequestFilter
        void record(HttpRequest<?> request) {
            request.getHeaders().findFirst("X-Api-Key").ifPresent(keys::add);
        }
    }

    @Requires(property = "spec.name", value = CLIENT)
    @Singleton
    static class HidingFilter implements McpToolFilter {
        @Override
        public boolean test(McpClient client, ToolSpecification tool) {
            return !tool.name().equals("hidden");
        }
    }

    @Requires(property = "spec.name", value = CLIENT)
    @Singleton
    static class RenamingBuilderListener implements BeanCreatedEventListener<McpToolProvider.Builder> {
        @Override
        public McpToolProvider.Builder onCreated(BeanCreatedEvent<McpToolProvider.Builder> event) {
            return event.getBean().toolNameMapper((client, tool) -> "listened_" + tool.name());
        }
    }

    @Requires(property = "spec.name", value = CLIENT)
    @Singleton
    static class ToolsListCounter implements McpClientListener {
        final AtomicInteger count = new AtomicInteger();

        @Override
        public void afterToolsList(McpCallContext context, List<ToolSpecification> tools) {
            count.incrementAndGet();
        }
    }
}
