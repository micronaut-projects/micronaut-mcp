package io.micronaut.mcp.client.langchain4j.health;

import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.mcp.client.langchain4j.ConnectionMcpClients;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpClientHealthIndicatorTest {

    @Test
    void anErrorWithoutAMessageIsReportedByItsType() {
        McpClient failing = client((proxy, method, args) -> {
            throw new IllegalStateException();
        });
        try (ApplicationContext context = ApplicationContext.run();
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            context.registerSingleton(McpClient.class, failing, Qualifiers.byName("broken"));
            ConnectionMcpClients.register(context, "broken");
            McpClientHealthIndicator indicator = indicator(context, executor, Duration.ofSeconds(10), "broken");
            assertEquals(Map.of("broken", Map.of("status", "DOWN", "error", IllegalStateException.class.getName())),
                indicator.getHealthInformation());
        }
    }

    @Test
    void aPingThatIsStillRunningIsAwaitedInsteadOfStartingAnother() throws InterruptedException {
        AtomicInteger pings = new AtomicInteger();
        CountDownLatch answer = new CountDownLatch(1);
        McpClient slow = client((proxy, method, args) -> {
            if (method.getName().equals("checkHealth")) {
                pings.incrementAndGet();
                answer.await();
            }
            return null;
        });
        try (ApplicationContext context = ApplicationContext.run();
             ExecutorService executor = Executors.newCachedThreadPool()) {
            context.registerSingleton(McpClient.class, slow, Qualifiers.byName("slow"));
            ConnectionMcpClients.register(context, "slow");
            McpClientHealthIndicator indicator = indicator(context, executor, Duration.ofMillis(100), "slow");
            Map<String, Object> down = Map.of("slow", Map.of("status", "DOWN", "error", "No answer within 100ms, the client may still be connecting to its server. See endpoints.health.mcp.timeout"));
            try {
                assertEquals(down, indicator.getHealthInformation());
                assertEquals(down, indicator.getHealthInformation());
                assertEquals(1, pings.get(), "The second check awaits the ping that is still running");
            } finally {
                // Lets the pings end, so that the executor can be closed
                answer.countDown();
            }
            assertEquals(Map.of("slow", Map.of("status", "UP")), awaitUp(indicator));
        }
    }

    @Test
    void connectionsWithTheSameNameAreRejected() {
        McpClientConnectionConfiguration connection = () -> "twice";
        try (ApplicationContext context = ApplicationContext.run();
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            List<McpClientConnectionConfiguration> connections = List.of(connection, connection);
            ConfigurationException e = assertThrows(ConfigurationException.class,
                () -> new McpClientHealthIndicator(connections, context, executor, configuration(Duration.ofSeconds(1))));
            assertTrue(e.getMessage().contains("have the same name 'twice'"), e.getMessage());
        }
    }

    @Test
    void theIndicatorCanBeDisabled() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.client.http.weather.url", "http://localhost:1/mcp",
            "endpoints.health.mcp.enabled", "false"))) {
            assertTrue(context.getBeansOfType(HealthIndicator.class).stream().noneMatch(McpClientHealthIndicator.class::isInstance));
        }
    }

    @Test
    void thereIsNoIndicatorWithoutConnections() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(context.getBeansOfType(HealthIndicator.class).stream().noneMatch(McpClientHealthIndicator.class::isInstance));
        }
    }

    @Test
    void theIndicatorIsEnabledByDefault() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.client.http.weather.url", "http://localhost:1/mcp"))) {
            assertTrue(context.getBeansOfType(HealthIndicator.class).stream().anyMatch(McpClientHealthIndicator.class::isInstance));
        }
    }

    private static Map<String, Object> awaitUp(McpClientHealthIndicator indicator) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            Map<String, Object> information = indicator.getHealthInformation();
            if (!information.toString().contains("DOWN")) {
                return information;
            }
            Thread.sleep(100);
        }
        return indicator.getHealthInformation();
    }

    private static McpClientHealthIndicator indicator(ApplicationContext context, ExecutorService executor, Duration timeout, String name) {
        McpClientConnectionConfiguration connection = () -> name;
        return new McpClientHealthIndicator(List.of(connection), context, executor, configuration(timeout));
    }

    private static McpClientHealthConfiguration configuration(Duration timeout) {
        McpClientHealthConfiguration configuration = new McpClientHealthConfiguration();
        configuration.setTimeout(timeout);
        return configuration;
    }

    private static McpClient client(InvocationHandler handler) {
        return (McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(), new Class<?>[]{McpClient.class}, handler);
    }
}
