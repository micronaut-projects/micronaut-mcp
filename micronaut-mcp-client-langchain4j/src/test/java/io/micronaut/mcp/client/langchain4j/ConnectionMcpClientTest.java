package io.micronaut.mcp.client.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.RuntimeBeanDefinition;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.exceptions.BeanInstantiationException;
import io.micronaut.context.exceptions.NoSuchBeanException;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectionMcpClientTest {

    @Test
    void everyCallIsDelegatedToTheClientOfTheConnection() {
        List<String> calls = new CopyOnWriteArrayList<>();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.registerSingleton(McpClient.class, recording(calls), Qualifiers.byName("fake"));
            ConnectionMcpClient client = new ConnectionMcpClient("fake", context);
            ToolExecutionRequest request = ToolExecutionRequest.builder().name("tool").arguments("{}").build();
            assertEquals("fake", client.key());
            client.instructions();
            client.listTools();
            client.listTools(null);
            client.executeTool(request);
            client.executeTool(request, null);
            client.executeToolAsync(request, null);
            client.listResources();
            client.listResources(null);
            client.listResourceTemplates();
            client.listResourceTemplates(null);
            client.readResource("uri");
            client.readResource("uri", null);
            client.subscribeToResource("uri");
            client.unsubscribeFromResource("uri");
            client.subscribeToResources(List.of("uri"));
            client.unsubscribeFromResources(1L);
            client.listPrompts();
            client.getPrompt("prompt", Map.of());
            client.checkHealth();
            client.setRoots(List.of());
            assertEquals(List.of("instructions", "listTools", "listTools", "executeTool", "executeTool", "executeToolAsync",
                "listResources", "listResources", "listResourceTemplates", "listResourceTemplates", "readResource",
                "readResource", "subscribeToResource", "unsubscribeFromResource", "subscribeToResources",
                "unsubscribeFromResources", "listPrompts", "getPrompt", "checkHealth", "setRoots"), calls);
        }
    }

    @Test
    void closingForgetsTheClientWithoutClosingIt() {
        List<List<String>> clients = new CopyOnWriteArrayList<>();
        try (ApplicationContext context = ApplicationContext.run()) {
            // A prototype, so that each resolution gives a new client
            context.registerBeanDefinition(RuntimeBeanDefinition.builder(McpClient.class, () -> {
                    List<String> calls = new CopyOnWriteArrayList<>();
                    clients.add(calls);
                    return recording(calls);
                })
                .qualifier(Qualifiers.byName("prototype"))
                .scope(Prototype.class)
                .build());
            ConnectionMcpClient client = new ConnectionMcpClient("prototype", context);
            client.checkHealth();
            client.checkHealth();
            assertEquals(1, clients.size(), "The client is resolved once");
            client.close();
            client.checkHealth();
            assertEquals(List.of(List.of("checkHealth", "checkHealth"), List.of("checkHealth")), clients,
                "The client is resolved again after close, and the first client is not closed");
        }
    }

    @Test
    void aClientWhoseCreationFailedIsCreatedAgain() {
        AtomicInteger attempts = new AtomicInteger();
        List<String> calls = new CopyOnWriteArrayList<>();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.registerBeanDefinition(RuntimeBeanDefinition.builder(McpClient.class, () -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("The server cannot be reached");
                    }
                    return recording(calls);
                })
                .qualifier(Qualifiers.byName("flaky"))
                .singleton(true)
                .build());
            ConnectionMcpClient client = new ConnectionMcpClient("flaky", context);
            assertThrows(BeanInstantiationException.class, client::checkHealth);
            client.checkHealth();
            assertEquals(2, attempts.get());
            assertEquals(List.of("checkHealth"), calls);
        }
    }

    @Test
    void theClientIsResolvedAgainUntilItCanBeCreated() {
        List<String> calls = new CopyOnWriteArrayList<>();
        try (ApplicationContext context = ApplicationContext.run()) {
            ConnectionMcpClient client = new ConnectionMcpClient("later", context);
            assertThrows(NoSuchBeanException.class, client::checkHealth);
            context.registerSingleton(McpClient.class, recording(calls), Qualifiers.byName("later"));
            client.checkHealth();
            assertEquals(List.of("checkHealth"), calls);
        }
    }

    private static McpClient recording(List<String> calls) {
        return (McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(), new Class<?>[]{McpClient.class}, (proxy, method, args) -> {
            calls.add(method.getName());
            Class<?> type = method.getReturnType();
            if (type == List.class) {
                return List.of();
            } else if (type == long.class) {
                return 1L;
            } else if (type == CompletableFuture.class) {
                return CompletableFuture.completedFuture(null);
            } else if (type == String.class) {
                return "fake";
            }
            return null;
        });
    }
}
