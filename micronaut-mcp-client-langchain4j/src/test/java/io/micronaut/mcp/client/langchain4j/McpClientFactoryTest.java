package io.micronaut.mcp.client.langchain4j;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpClientFactoryTest {
    private final McpClientFactory factory = new McpClientFactory();

    @Test
    void theSettingsOfTheConnectionAreAppliedToTheBuilder() throws ReflectiveOperationException {
        McpClientListener listener = new McpClientListener() { };
        McpLogMessageHandler logHandler = message -> { };
        DefaultMcpClient.Builder builder = factory.crateMcpClientBuilder(connection(Duration.ofSeconds(5), Duration.ofSeconds(7), false),
            List.of(listener), logHandler);
        assertEquals("weather", field(builder, "key"));
        assertEquals(Boolean.FALSE, field(builder, "autoHealthCheck"));
        assertEquals(Duration.ofSeconds(5), field(builder, "initializationTimeout"));
        for (String timeout : List.of("toolExecutionTimeout", "resourcesTimeout", "promptsTimeout", "pingTimeout")) {
            assertEquals(Duration.ofSeconds(7), field(builder, timeout), timeout);
        }
        assertEquals(List.of(listener), field(builder, "listeners"));
        assertSame(logHandler, field(builder, "logHandler"));
    }

    @Test
    void unsetSettingsLeaveTheDefaultsOfTheClient() throws ReflectiveOperationException {
        DefaultMcpClient.Builder builder = factory.crateMcpClientBuilder(connection(null, null, true), List.of(), null);
        for (String timeout : List.of("initializationTimeout", "toolExecutionTimeout", "resourcesTimeout", "promptsTimeout", "pingTimeout", "logHandler")) {
            assertNull(field(builder, timeout), timeout);
        }
        assertEquals(Boolean.TRUE, field(builder, "autoHealthCheck"));
    }

    @Test
    void theTransportOfAClientThatCannotBeCreatedIsClosed() {
        AtomicBoolean closed = new AtomicBoolean();
        IllegalStateException failure = new IllegalStateException("The server cannot be started");
        McpTransport transport = transport(failure, closed, null);
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> factory.createMcpClient(connection(null, null, false), new DefaultMcpClient.Builder(), transport));
        assertSame(failure, thrown);
        assertTrue(closed.get(), "The transport is closed");
    }

    @Test
    void aTransportThatFailsToCloseDoesNotHideTheFailure() {
        AtomicBoolean closed = new AtomicBoolean();
        IllegalStateException failure = new IllegalStateException("The server cannot be started");
        McpTransport transport = transport(failure, closed, new IOException("The transport cannot be closed"));
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> factory.createMcpClient(connection(null, null, false), new DefaultMcpClient.Builder(), transport));
        assertSame(failure, thrown);
        assertTrue(closed.get(), "Closing the transport is attempted");
    }

    private static McpTransport transport(RuntimeException startFailure, AtomicBoolean closed, IOException closeFailure) {
        return (McpTransport) Proxy.newProxyInstance(McpTransport.class.getClassLoader(), new Class<?>[]{McpTransport.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "start" -> throw startFailure;
                case "close" -> {
                    closed.set(true);
                    if (closeFailure != null) {
                        throw closeFailure;
                    }
                    yield null;
                }
                default -> null;
            });
    }

    private static McpClientConnectionConfiguration connection(Duration initializationTimeout, Duration requestTimeout, boolean autoHealthCheck) {
        return new McpClientConnectionConfiguration() {
            @Override
            public @NonNull String getName() {
                return "weather";
            }

            @Override
            public Duration getInitializationTimeout() {
                return initializationTimeout;
            }

            @Override
            public Duration getRequestTimeout() {
                return requestTimeout;
            }

            @Override
            public boolean isAutoHealthCheck() {
                return autoHealthCheck;
            }
        };
    }

    // The builder exposes no getters
    private static Object field(DefaultMcpClient.Builder builder, String name) throws ReflectiveOperationException {
        Field field = DefaultMcpClient.Builder.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(builder);
    }
}
