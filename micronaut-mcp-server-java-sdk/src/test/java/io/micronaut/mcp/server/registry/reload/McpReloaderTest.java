package io.micronaut.mcp.server.registry.reload;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.DefaultBeanContext;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.PropertySource;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.micronaut.runtime.context.scope.refresh.ConfigurationRefresher;
import io.micronaut.runtime.context.scope.refresh.RefreshResult;
import io.micronaut.runtime.server.EmbeddedServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Changes the annotated methods of a running context, as a definition registered at runtime or a development reload
 * does, and checks that the MCP server lists what the context now has, and tells its clients.
 */
class McpReloaderTest {

    private static final String SPEC = "McpReloaderTest";
    private static final String RELOADER = "io.micronaut.mcp.server.registry.DevelopmentMcpReloader";
    private static final String TOOLS_CHANGED = "notifications/tools/list_changed";
    private static final String PROMPTS_CHANGED = "notifications/prompts/list_changed";
    private static final String RESOURCES_CHANGED = "notifications/resources/list_changed";

    private ApplicationContext context;

    @AfterEach
    void close() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void primitivesAddedAtRuntimeAreListedAndTheClientsAreToldTheListsChanged() throws ReflectiveOperationException {
        context = start(true, "STDIO");
        McpSyncServer server = context.getBean(McpSyncServer.class);
        RecordingTransport transport = context.getBean(RecordingTransport.class);

        assertTrue(context.containsBean(reloader()));
        assertTrue(toolNames(server).contains("hello"));
        assertFalse(toolNames(server).contains("extra"));

        // a definition the context did not have at startup: its methods come as additions
        definitions().notifyDefinitionChange(List.of(), List.of(extraDefinition()));

        assertTrue(toolNames(server).contains("extra"));
        assertTrue(server.listPrompts().stream().anyMatch(p -> p.name().equals("extra-prompt")));
        assertTrue(server.listResources().stream().anyMatch(r -> r.uri().equals("reload://extra")));
        assertTrue(transport.notifications.containsAll(List.of(TOOLS_CHANGED, PROMPTS_CHANGED, RESOURCES_CHANGED)));

        // the registries of the context, which the context also feeds what is added, keep none of it
        assertTrue(registeredMethods(context.getBean(ToolRegistry.class)).isEmpty());
        assertTrue(registeredMethods(context.getBean(PromptRegistry.class)).isEmpty());
        assertTrue(registeredMethods(context.getBean(ResourceRegistry.class)).isEmpty());
    }

    @Test
    void aRemovedToolIsNoLongerListedAndARedefinedOneIsListedOnce() throws ReflectiveOperationException {
        context = start(true, "STDIO");
        McpSyncServer server = context.getBean(McpSyncServer.class);
        RecordingTransport transport = context.getBean(RecordingTransport.class);
        BeanDefinition<ReloadTools> definition = context.getBeanDefinition(ReloadTools.class);

        // retired and replaced, as a reload applied in place replaces the definitions of the reloaded classes
        definitions().notifyDefinitionChange(List.of(definition), List.of(definition));

        assertEquals(1, toolNames(server).stream().filter("hello"::equals).count());
        assertTrue(transport.notifications.contains(TOOLS_CHANGED));

        transport.notifications.clear();
        definitions().notifyDefinitionChange(List.of(definition), List.of());

        assertFalse(toolNames(server).contains("hello"));
        assertTrue(transport.notifications.contains(TOOLS_CHANGED));
        assertTrue(registeredMethods(context.getBean(ToolRegistry.class)).isEmpty());
    }

    @Test
    void theStatelessHttpServerListsWhatTheContextNowHas() throws ReflectiveOperationException {
        context = start(true, "HTTP");
        EmbeddedServer embeddedServer = context.getBean(EmbeddedServer.class).start();
        McpStatelessSyncServer server = context.getBean(McpStatelessSyncServer.class);
        BeanDefinition<ReloadTools> definition = context.getBeanDefinition(ReloadTools.class);

        try (HttpClient client = context.createBean(HttpClient.class, embeddedServer.getURL())) {
            assertTrue(listTools(client).contains("\"hello\""));

            definitions().notifyDefinitionChange(List.of(definition), List.of(extraDefinition()));

            String tools = listTools(client);
            assertFalse(tools.contains("\"hello\""));
            assertTrue(tools.contains("\"extra\""));
            assertTrue(server.listResources().stream().anyMatch(r -> r.uri().equals("reload://extra")));
        }
    }

    @Test
    void aChangeOfTheServerConfigurationAsksForARestart() {
        context = start(true, "STDIO");
        ConfigurationRefresher refresher = context.getBean(ConfigurationRefresher.class);
        McpSyncServer server = context.getBean(McpSyncServer.class);

        context.getEnvironment().addPropertySource(PropertySource.of("edit", Map.of("micronaut.mcp.server.info.name", "renamed"), Integer.MAX_VALUE));
        RefreshResult refresh = refresher.refresh();

        assertTrue(refresh.requiresRestart());
        assertTrue(context.getBean(McpSyncServer.class) == server);
    }

    @Test
    void outsideDevelopmentModeThereIsNoReloaderAndTheServerKeepsWhatItWasBuiltWith() {
        context = start(false, "STDIO");
        McpSyncServer server = context.getBean(McpSyncServer.class);
        RecordingTransport transport = context.getBean(RecordingTransport.class);
        BeanDefinition<ReloadTools> definition = context.getBeanDefinition(ReloadTools.class);

        assertFalse(context.containsBean(reloader()));

        definitions().notifyDefinitionChange(List.of(definition), List.of());

        assertTrue(toolNames(server).contains("hello"));
        assertTrue(transport.notifications.isEmpty());
    }

    private ApplicationContext start(boolean development, String transport) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spec.name", SPEC);
        properties.put("micronaut.mcp.server.transport", transport);
        properties.put("micronaut.mcp.server.info.name", "reload");
        properties.put("micronaut.mcp.server.info.version", "1.0.0");
        properties.put("micronaut.mcp.server.tools.list-changed", true);
        properties.put("micronaut.mcp.server.prompts.list-changed", true);
        properties.put("micronaut.mcp.server.resources.list-changed", true);
        properties.put("micronaut.server.port", -1);
        if (development) {
            properties.put("micronaut.dev.enabled", true);
        }
        ApplicationContext started = ApplicationContext.builder().properties(properties).start();
        // created first: it compares the next refresh against the configuration as it is now
        started.getBean(ConfigurationRefresher.class);
        return started;
    }

    private static List<?> registeredMethods(Object registry) throws ReflectiveOperationException {
        Field methods = registry.getClass().getSuperclass().getDeclaredField("methods");
        methods.setAccessible(true);
        return (List<?>) methods.get(registry);
    }

    private DefaultBeanContext definitions() {
        return (DefaultBeanContext) context;
    }

    private static List<String> toolNames(McpSyncServer server) {
        return server.listTools().stream().map(McpSchema.Tool::name).toList();
    }

    private static String listTools(HttpClient client) {
        HttpRequest<?> request = HttpRequest.POST("/mcp", Map.of("jsonrpc", "2.0", "id", 1, "method", "tools/list"))
            .accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_EVENT_STREAM_TYPE);
        return client.toBlocking().retrieve(request);
    }

    /**
     * @return The definition of a bean the context did not load at startup
     */
    @SuppressWarnings("unchecked")
    private static BeanDefinition<ExtraTools> extraDefinition() throws ReflectiveOperationException {
        Class<?> type = Class.forName(McpReloaderTest.class.getPackageName() + ".$McpReloaderTest$ExtraTools$Definition");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (BeanDefinition<ExtraTools>) constructor.newInstance();
    }

    private static Class<?> reloader() {
        try {
            return Class.forName(RELOADER);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Singleton
    @Requires(property = "spec.name", value = SPEC)
    static class ReloadTools {
        @Tool(name = "hello", description = "Says hello")
        String hello() {
            return "hello";
        }

        // the server has the prompts and resources capabilities only if it starts with some
        @Prompt(name = "hello-prompt", description = "A prompt")
        String helloPrompt() {
            return "hello";
        }

        @Resource(uri = "reload://hello", name = "hello", description = "A resource")
        String helloResource() {
            return "hello";
        }
    }

    /**
     * Never loaded at startup: its definition is handed to the context as a reload would.
     */
    @Singleton
    @Requires(property = "spec.name", value = SPEC)
    @Requires(property = "reload.extra", value = "true")
    static class ExtraTools {
        @Tool(name = "extra", description = "An extra tool")
        String extra() {
            return "extra";
        }

        @Prompt(name = "extra-prompt", description = "An extra prompt")
        String extraPrompt() {
            return "extra";
        }

        @Resource(uri = "reload://extra", name = "extra", description = "An extra resource")
        String extraResource() {
            return "extra";
        }
    }

    /**
     * Stands for the sessions of a server over STDIO: the SDK sends the notifications for all clients here.
     */
    @Singleton
    @Requires(property = "spec.name", value = SPEC)
    @Requires(property = "micronaut.mcp.server.transport", value = "STDIO")
    @Replaces(McpServerTransportProvider.class)
    static class RecordingTransport implements McpServerTransportProvider {
        final List<String> notifications = new CopyOnWriteArrayList<>();

        @Override
        public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        }

        @Override
        public Mono<Void> notifyClients(String method, Object params) {
            notifications.add(method);
            return Mono.empty();
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.empty();
        }
    }
}
