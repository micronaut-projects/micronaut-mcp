package io.micronaut.mcp.client.langchain4j.health;

import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpClientHealthIndicatorTest {

    @Test
    void anErrorWithoutAMessageIsReportedByItsType() {
        McpClient failing = (McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(), new Class<?>[]{McpClient.class},
            (proxy, method, args) -> {
                throw new IllegalStateException();
            });
        McpClientConnectionConfiguration connection = () -> "broken";
        try (ApplicationContext context = ApplicationContext.run();
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            context.registerSingleton(McpClient.class, failing, Qualifiers.byName("broken"));
            McpClientHealthIndicator indicator = new McpClientHealthIndicator(List.of(connection), context, executor, Duration.ofSeconds(10));
            assertEquals(Map.of("broken", Map.of("status", "DOWN", "error", IllegalStateException.class.getName())),
                indicator.getHealthInformation());
        }
    }
}
