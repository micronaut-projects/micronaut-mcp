package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the settings and beans that customise the clients and the tool providers of HTTP connections, with two
 * connections to the same server.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpConnectionCustomisationTest {
    private static final String SERVER = "HttpConnectionCustomisationTest-server";
    private static final String CLIENT = "HttpConnectionCustomisationTest-client";
    private static final String NO_KEY = "<none>";

    private EmbeddedServer mcpServer;
    private ApplicationContext client;

    @BeforeAll
    void start() {
        mcpServer = ApplicationContext.run(EmbeddedServer.class, Map.of(
            "spec.name", SERVER,
            "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.info.name", "customisation",
            "micronaut.mcp.server.transport", "HTTP"));
        String url = mcpServer.getURL().toString() + mcpServer.getApplicationContext().getBean(McpServerConfiguration.class).getEndpoint();
        client = ApplicationContext.run(Map.of(
            "spec.name", CLIENT,
            "micronaut.mcp.client.http.remote.url", url,
            "micronaut.mcp.client.http.remote.headers.X-Api-Key", "secret",
            "micronaut.mcp.client.http.remote.request-timeout", "500ms",
            "micronaut.mcp.client.http.other.url", url,
            "micronaut.mcp.client.http.other.headers.X-Api-Key", "other"));
    }

    @AfterAll
    void stop() {
        client.close();
        mcpServer.close();
    }

    @Test
    void theFilterAndTheBuilderListenerApplyToThePrimaryProvider() {
        assertEquals(Set.of("listened_remote_forecast", "listened_remote_slow", "listened_other_forecast", "listened_other_slow"),
            toolNames(client.getBean(McpToolProvider.class)));
    }

    @Test
    void theFilterAndTheBuilderListenerApplyToTheProviderOfEachConnection() {
        assertEquals(Set.of("listened_remote_forecast", "listened_remote_slow"),
            toolNames(client.getBean(McpToolProvider.class, Qualifiers.byName("remote"))));
        assertEquals(Set.of("listened_other_forecast", "listened_other_slow"),
            toolNames(client.getBean(McpToolProvider.class, Qualifiers.byName("other"))));
    }

    @Test
    void eachConnectionSendsItsOwnHeaderWithEveryRequest() {
        client.getBean(McpClient.class, Qualifiers.byName("remote")).listTools();
        client.getBean(McpClient.class, Qualifiers.byName("other")).listTools();
        List<String> keys = mcpServer.getApplicationContext().getBean(ApiKeyRecorder.class).keys;
        assertTrue(keys.stream().noneMatch(NO_KEY::equals), "Every request carries the header of its connection: " + keys);
        assertEquals(Set.of("secret", "other"), Set.copyOf(keys));
    }

    @Test
    void theListenerIsRegisteredWithTheClient() {
        ToolsListCounter counter = client.getBean(ToolsListCounter.class);
        DefaultMcpClient other = (DefaultMcpClient) client.getBean(McpClient.class, Qualifiers.byName("other"));
        // The client caches the tool list
        other.evictToolListCache();
        int before = counter.count.get();
        other.listTools();
        assertEquals(before + 1, counter.count.get(), "The listener is notified once of the tool list");
    }

    @Test
    void theRequestTimeoutLimitsAToolCall() {
        McpClient remote = client.getBean(McpClient.class, Qualifiers.byName("remote"));
        remote.listTools();
        long start = System.nanoTime();
        String result = remote.executeTool(ToolExecutionRequest.builder().name("slow").arguments("{}").build()).resultText();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertNotEquals("done", result);
        assertTrue(elapsedMillis < ServerTools.SLOW_MILLIS, "The call ends with the request timeout, after " + elapsedMillis + "ms");
    }

    private static Set<String> toolNames(McpToolProvider provider) {
        return provider.provideTools(new ToolProviderRequest("memory", UserMessage.from("hi"))).tools().keySet().stream()
            .map(ToolSpecification::name)
            .collect(Collectors.toSet());
    }

    @Requires(property = "spec.name", value = SERVER)
    @Singleton
    static class ServerTools {
        static final long SLOW_MILLIS = 3000;

        @Tool(description = "Forecasts the weather")
        String forecast(String city) {
            return "sunny";
        }

        @Tool(description = "A tool that the filter hides")
        String hidden() {
            return "hidden";
        }

        @Tool(description = "A tool slower than the request timeout of a connection")
        String slow() throws InterruptedException {
            Thread.sleep(SLOW_MILLIS);
            return "done";
        }
    }

    @Requires(property = "spec.name", value = SERVER)
    @ServerFilter("/mcp/**")
    static class ApiKeyRecorder {
        final List<String> keys = new CopyOnWriteArrayList<>();

        @RequestFilter
        void record(HttpRequest<?> request) {
            keys.add(request.getHeaders().findFirst("X-Api-Key").orElse(NO_KEY));
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
            return event.getBean().toolNameMapper((client, tool) -> "listened_" + client.key() + "_" + tool.name());
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
