package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invokes the handlers of every specification variant the registries build: STDIO sync and async, with server
 * exchanges, and stateless sync and async, with transport contexts.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "RegistryHandlersTest")
@MicronautTest(startApplication = false)
class RegistryHandlersTest {
    private static final McpTransportContext CONTEXT = McpTransportContext.create(Map.of());
    private static final McpAsyncServerExchange ASYNC_EXCHANGE = new McpAsyncServerExchange("session", null,
        McpSchema.ClientCapabilities.builder().build(), new McpSchema.Implementation("client", "1.0"), CONTEXT);
    private static final McpSyncServerExchange SYNC_EXCHANGE = new McpSyncServerExchange(ASYNC_EXCHANGE);

    @Inject
    ToolRegistry toolRegistry;

    @Inject
    PromptRegistry promptRegistry;

    @Inject
    ResourceRegistry resourceRegistry;

    @Inject
    ResourceTemplateRegistry resourceTemplateRegistry;

    @Inject
    CompletionRegistry completionRegistry;

    @Test
    void tools() {
        McpSchema.CallToolRequest echo = new McpSchema.CallToolRequest("echo", Map.of("text", "hi"));
        assertEquals("hi", text(toolRegistry.getSyncSpecs().stream().filter(s -> s.tool().name().equals("echo")).findFirst().orElseThrow()
            .callHandler().apply(SYNC_EXCHANGE, echo)));
        assertEquals("hi", text(toolRegistry.getAsyncSpecs().stream().filter(s -> s.tool().name().equals("echo")).findFirst().orElseThrow()
            .callHandler().apply(ASYNC_EXCHANGE, echo).block()));
        assertEquals("hi", text(toolRegistry.getStatelessSyncSpecs().stream().filter(s -> s.tool().name().equals("echo")).findFirst().orElseThrow()
            .callHandler().apply(CONTEXT, echo)));
        assertEquals("hi", text(toolRegistry.getStatelessAsyncSpecs().stream().filter(s -> s.tool().name().equals("echo")).findFirst().orElseThrow()
            .callHandler().apply(CONTEXT, echo).block()));
        // a prototype bean is looked up for every call
        McpSchema.CallToolRequest counter = new McpSchema.CallToolRequest("count", Map.of());
        var count = toolRegistry.getStatelessSyncSpecs().stream().filter(s -> s.tool().name().equals("count")).findFirst().orElseThrow().callHandler();
        assertEquals("1", text(count.apply(CONTEXT, counter)));
        assertEquals("1", text(count.apply(CONTEXT, counter)));
    }

    @Test
    void failingToolsAreErrors() {
        var failing = toolRegistry.getStatelessSyncSpecs().stream().filter(s -> s.tool().name().equals("failing")).findFirst().orElseThrow().callHandler();
        // an exception without a mapper, twice, so the second lookup hits the cached miss
        assertTrue(isError(() -> failing.apply(CONTEXT, new McpSchema.CallToolRequest("failing", Map.of()))));
        assertTrue(isError(() -> failing.apply(CONTEXT, new McpSchema.CallToolRequest("failing", Map.of()))));
        // a missing argument, which an exception mapper maps
        var echo = toolRegistry.getStatelessSyncSpecs().stream().filter(s -> s.tool().name().equals("echo")).findFirst().orElseThrow().callHandler();
        assertTrue(isError(() -> echo.apply(CONTEXT, new McpSchema.CallToolRequest("echo", Map.of()))));
    }

    @Test
    void parametersReceivingTheContextAreNotToolArguments() {
        var spec = toolRegistry.getStatelessSyncSpecs().stream().filter(s -> s.tool().name().equals("contextual")).findFirst().orElseThrow();
        // an Object parameter is bound to the transport context, so it is not advertised in the input schema
        assertEquals(Set.of("text"), ((Map<?, ?>) spec.tool().inputSchema().get("properties")).keySet());
        assertEquals("hi true", text(spec.callHandler().apply(CONTEXT, new McpSchema.CallToolRequest("contextual", Map.of("text", "hi")))));
    }

    @Test
    void prompts() {
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("greeting", Map.of());
        assertEquals("Hello", prompt(promptRegistry.getSyncSpecs().stream().filter(s -> s.prompt().name().equals("greeting")).findFirst().orElseThrow()
            .promptHandler().apply(SYNC_EXCHANGE, request)));
        assertEquals("Hello", prompt(promptRegistry.getAsyncSpecs().stream().filter(s -> s.prompt().name().equals("greeting")).findFirst().orElseThrow()
            .promptHandler().apply(ASYNC_EXCHANGE, request).block()));
        assertEquals("Hello", prompt(promptRegistry.getStatelessSyncSpecs().stream().filter(s -> s.prompt().name().equals("greeting")).findFirst().orElseThrow()
            .promptHandler().apply(CONTEXT, request)));
        assertEquals("Hello", prompt(promptRegistry.getStatelessAsyncSpecs().stream().filter(s -> s.prompt().name().equals("greeting")).findFirst().orElseThrow()
            .promptHandler().apply(CONTEXT, request).block()));
    }

    @Test
    void resources() {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("handlers://readme");
        assertEquals("readme", resource(resourceRegistry.getSyncSpecs().stream().filter(s -> s.resource().uri().equals("handlers://readme")).findFirst().orElseThrow()
            .readHandler().apply(SYNC_EXCHANGE, request)));
        assertEquals("readme", resource(resourceRegistry.getAsyncSpecs().stream().filter(s -> s.resource().uri().equals("handlers://readme")).findFirst().orElseThrow()
            .readHandler().apply(ASYNC_EXCHANGE, request).block()));
        assertEquals("readme", resource(resourceRegistry.getStatelessSyncSpecs().stream().filter(s -> s.resource().uri().equals("handlers://readme")).findFirst().orElseThrow()
            .readHandler().apply(CONTEXT, request)));
        assertEquals("readme", resource(resourceRegistry.getStatelessAsyncSpecs().stream().filter(s -> s.resource().uri().equals("handlers://readme")).findFirst().orElseThrow()
            .readHandler().apply(CONTEXT, request).block()));
    }

    @Test
    void resourceTemplates() {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("handlers://pages/7");
        String template = "handlers://pages/{page}";
        assertEquals("page 7", resource(resourceTemplateRegistry.getSyncSpecs().stream().filter(s -> s.resourceTemplate().uriTemplate().equals(template)).findFirst().orElseThrow()
            .readHandler().apply(SYNC_EXCHANGE, request)));
        assertEquals("page 7", resource(resourceTemplateRegistry.getAsyncSpecs().stream().filter(s -> s.resourceTemplate().uriTemplate().equals(template)).findFirst().orElseThrow()
            .readHandler().apply(ASYNC_EXCHANGE, request).block()));
        assertEquals("page 7", resource(resourceTemplateRegistry.getStatelessSyncSpecs().stream().filter(s -> s.resourceTemplate().uriTemplate().equals(template)).findFirst().orElseThrow()
            .readHandler().apply(CONTEXT, request)));
        assertEquals("page 7", resource(resourceTemplateRegistry.getStatelessAsyncSpecs().stream().filter(s -> s.resourceTemplate().uriTemplate().equals(template)).findFirst().orElseThrow()
            .readHandler().apply(CONTEXT, request).block()));
    }

    @Test
    void completions() {
        McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(new McpSchema.PromptReference("greeting"),
            new McpSchema.CompleteRequest.CompleteArgument("name", "A"));
        assertEquals(List.of("Alice"), completionRegistry.getSyncSpecs().getFirst().completionHandler().apply(SYNC_EXCHANGE, request).completion().values());
        assertEquals(List.of("Alice"), completionRegistry.getAsyncSpecs().getFirst().completionHandler().apply(ASYNC_EXCHANGE, request).block().completion().values());
        assertEquals(List.of("Alice"), completionRegistry.getStatelessSyncSpecs().getFirst().completionHandler().apply(CONTEXT, request).completion().values());
        assertEquals(List.of("Alice"), completionRegistry.getStatelessAsyncSpecs().getFirst().completionHandler().apply(CONTEXT, request).block().completion().values());
    }

    private static boolean isError(Supplier<McpSchema.CallToolResult> call) {
        try {
            // a protocol error, or a tool execution error
            return Boolean.TRUE.equals(call.get().isError());
        } catch (McpError _) {
            return true;
        }
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    private static String prompt(McpSchema.GetPromptResult result) {
        return ((McpSchema.TextContent) result.messages().getFirst().content()).text();
    }

    private static String resource(McpSchema.ReadResourceResult result) {
        return ((McpSchema.TextResourceContents) result.contents().getFirst()).text();
    }

    @Requires(property = "spec.name", value = "RegistryHandlersTest")
    @Singleton
    static class Primitives {
        @Tool
        String echo(String text) {
            return text;
        }

        @Tool
        String contextual(String text, Object context) {
            return text + " " + (context instanceof McpTransportContext);
        }

        @Tool
        String failing() {
            throw new IllegalStateException("failing");
        }

        @Prompt
        String greeting() {
            return "Hello";
        }

        @PromptCompletion(name = "greeting")
        List<String> greetingCompletion(McpSchema.CompleteRequest.CompleteArgument argument) {
            return List.of("Alice");
        }

        @Resource(uri = "handlers://readme")
        String readme() {
            return "readme";
        }

        @ResourceTemplate(uriTemplate = "handlers://pages/{page}")
        String page(String page) {
            return "page " + page;
        }
    }

    @Requires(property = "spec.name", value = "RegistryHandlersTest")
    @Prototype
    static class Counter {
        private int count;

        @Tool
        String count() {
            return String.valueOf(++count);
        }
    }
}
