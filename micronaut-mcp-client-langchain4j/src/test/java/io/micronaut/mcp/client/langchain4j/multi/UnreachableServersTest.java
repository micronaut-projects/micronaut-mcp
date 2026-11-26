package io.micronaut.mcp.client.langchain4j.multi;

import dev.langchain4j.mcp.McpToolProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.mcpHealth;
import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.stdioConnection;
import static io.micronaut.mcp.client.langchain4j.multi.MultiClientTestSupport.toolNames;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "UnreachableServersTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnreachableServersTest implements TestPropertyProvider {

    @Inject
    McpToolProvider toolProvider;

    @Inject
    @Named("unreachable")
    McpToolProvider unreachableToolProvider;

    @Inject
    List<HealthIndicator> healthIndicators;

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("micronaut.mcp.client.http.unreachable.url", "http://localhost:" + freePort() + "/mcp");
        properties.put("micronaut.mcp.client.http.unreachable.initialization-timeout", "2s");
        properties.put("micronaut.mcp.client.http.unreachable.auto-health-check", "false");
        properties.put("micronaut.mcp.client.http.unreachable.headers.X-Test", "test");
        // Shorter than the initialization timeout of the clients
        properties.put("endpoints.health.mcp.timeout", "1s");
        properties.putAll(stdioConnection("hanging", HangingServer.class));
        properties.put("micronaut.mcp.client.stdio.hanging.initialization-timeout", "2s");
        return properties;
    }

    @Test
    void theToolProviderSkipsTheServersThatCannotBeReached() {
        assertEquals(Set.of("forecast"), toolNames(toolProvider));
        assertFalse(hangingServerRunning(), "The process of a client that could not be created is stopped");
    }

    @Test
    void theToolProviderOfAClientWhoseServerCannotBeReachedProvidesNoTools() {
        assertEquals(Set.of(), toolNames(unreachableToolProvider));
    }

    @Test
    void theHealthIndicatorReportsTheServersThatCannotBeReachedDown() {
        HealthResult mcp = mcpHealth(healthIndicators);
        assertEquals(HealthStatus.DOWN, mcp.getStatus());
        Map<?, ?> details = assertInstanceOf(Map.class, mcp.getDetails());
        assertEquals("UP", details.get("embeddedServer"));
        assertEquals("DOWN", assertInstanceOf(Map.class, details.get("unreachable")).get("status"));
        Map<?, ?> hanging = assertInstanceOf(Map.class, details.get("hanging"));
        assertEquals("DOWN", hanging.get("status"));
        assertEquals("No answer within PT1S", hanging.get("error"));
        assertFalse(hangingServerRunning(), "The process of a client that could not be created is stopped");
    }

    private static boolean hangingServerRunning() {
        // The process is stopped asynchronously
        return ProcessHandle.current().descendants()
            .filter(process -> process.info().arguments().map(args -> List.of(args).contains(HangingServer.class.getName())).orElse(false))
            .anyMatch(UnreachableServersTest::stillAlive);
    }

    private static boolean stillAlive(ProcessHandle process) {
        try {
            process.onExit().get(10, TimeUnit.SECONDS);
            return false;
        } catch (TimeoutException _) {
            return true;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return true;
        } catch (ExecutionException _) {
            return false;
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Requires(property = "spec.name", value = "UnreachableServersTest")
    @Singleton
    static class HttpTools {
        @Tool(description = "Forecasts the weather")
        String forecast(String city) {
            return "sunny";
        }
    }
}
