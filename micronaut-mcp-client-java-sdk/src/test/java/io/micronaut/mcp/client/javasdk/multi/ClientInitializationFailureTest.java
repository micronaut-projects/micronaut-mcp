package io.micronaut.mcp.client.javasdk.multi;

import io.micronaut.context.BeanContext;
import io.micronaut.context.Qualifier;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.BeanInstantiationException;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.client.javasdk.McpClientTool;
import io.micronaut.mcp.client.javasdk.McpClientTools;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "moon.enabled", value = "false")
@Property(name = "spec.name", value = "ClientInitializationFailureTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientInitializationFailureTest implements TestPropertyProvider {

    @Inject
    BeanContext beanContext;

    @Inject
    McpClientTools tools;

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        List<String> command = List.of(ProcessHandle.current().info().command().orElse("java"),
            "-cp", System.getProperty("java.class.path"), HangingServer.class.getName());
        for (int i = 0; i < command.size(); i++) {
            properties.put("micronaut.mcp.client.stdio.hanging.command[" + i + "]", command.get(i));
        }
        properties.put("micronaut.mcp.client.stdio.hanging.initialization-timeout", "2s");
        return properties;
    }

    @Test
    void aClientThatFailsToInitializeIsClosedAndCreatedAgainOnTheNextAttempt() {
        Qualifier<McpSyncClient> hanging = Qualifiers.byName("hanging");
        assertThrows(BeanInstantiationException.class, () -> beanContext.getBean(McpSyncClient.class, hanging));
        assertFalse(hangingServerRunning(), "The process of a client that failed to initialize is stopped");
        assertThrows(BeanInstantiationException.class, () -> beanContext.getBean(McpSyncClient.class, hanging));
        assertFalse(hangingServerRunning(), "The process of a client that failed to initialize is stopped");
    }

    @Test
    void thePrimaryToolsSkipTheServersThatCannotBeReached() {
        assertEquals(Set.of("forecast"), tools.tools().stream().map(McpClientTool::name).collect(Collectors.toSet()));
    }

    private static boolean hangingServerRunning() {
        // The process is stopped asynchronously
        return ProcessHandle.current().descendants()
            .filter(process -> process.info().arguments().map(args -> List.of(args).contains(HangingServer.class.getName())).orElse(false))
            .anyMatch(ClientInitializationFailureTest::stillAlive);
    }

    private static boolean stillAlive(ProcessHandle process) {
        try {
            process.onExit().get(10, TimeUnit.SECONDS);
            return false;
        } catch (TimeoutException e) {
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        } catch (ExecutionException e) {
            return false;
        }
    }

    @Requires(property = "spec.name", value = "ClientInitializationFailureTest")
    @Singleton
    static class HttpTools {
        @Tool(description = "Forecasts the weather")
        String forecast(String city) {
            return "sunny";
        }
    }
}
