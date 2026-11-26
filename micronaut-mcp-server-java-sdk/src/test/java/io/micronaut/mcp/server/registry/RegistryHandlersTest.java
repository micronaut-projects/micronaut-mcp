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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals("hi", text(find(toolRegistry.getSyncSpecs(), s -> s.tool().name().equals("echo"))
            .callHandler().apply(SYNC_EXCHANGE, echo)));
        assertEquals("hi", text(find(toolRegistry.getAsyncSpecs(), s -> s.tool().name().equals("echo"))
            .callHandler().apply(ASYNC_EXCHANGE, echo).block()));
        assertEquals("hi", text(find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("echo"))
            .callHandler().apply(CONTEXT, echo)));
        assertEquals("hi", text(find(toolRegistry.getStatelessAsyncSpecs(), s -> s.tool().name().equals("echo"))
            .callHandler().apply(CONTEXT, echo).block()));
        // a prototype bean is looked up for every call
        McpSchema.CallToolRequest counter = new McpSchema.CallToolRequest("count", Map.of());
        var count = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("count")).callHandler();
        assertEquals("1", text(count.apply(CONTEXT, counter)));
        assertEquals("1", text(count.apply(CONTEXT, counter)));
    }

    @Test
    void failingToolsAreErrors() {
        var failing = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("failing")).callHandler();
        McpSchema.CallToolRequest failingRequest = new McpSchema.CallToolRequest("failing", Map.of());
        // an exception without a mapper, twice, so the second lookup hits the cached miss
        assertError(McpSchema.ErrorCodes.INTERNAL_ERROR, "failing", () -> failing.apply(CONTEXT, failingRequest));
        assertError(McpSchema.ErrorCodes.INTERNAL_ERROR, "failing", () -> failing.apply(CONTEXT, failingRequest));
        // an exception without a message is reported with its type, because the SDK requires a message
        var silent = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("silent")).callHandler();
        assertError(McpSchema.ErrorCodes.INTERNAL_ERROR, "IllegalStateException",
            () -> silent.apply(CONTEXT, new McpSchema.CallToolRequest("silent", Map.of())));
        // an exception of an anonymous class, without a message, is reported with the name of its class
        var anonymous = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("anonymous")).callHandler();
        McpError anonymousError = assertThrows(McpError.class,
            () -> anonymous.apply(CONTEXT, new McpSchema.CallToolRequest("anonymous", Map.of())));
        assertEquals(McpSchema.ErrorCodes.INTERNAL_ERROR, anonymousError.getJsonRpcError().code());
        assertTrue(anonymousError.getJsonRpcError().message().startsWith(Primitives.class.getName() + "$"));
        // an McpError the tool throws keeps its code and message
        var invalid = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("invalid")).callHandler();
        assertError(McpSchema.ErrorCodes.INVALID_PARAMS, "bad",
            () -> invalid.apply(CONTEXT, new McpSchema.CallToolRequest("invalid", Map.of())));
        // a missing argument, which an exception mapper maps
        var echo = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("echo")).callHandler();
        McpError missing = assertThrows(McpError.class, () -> echo.apply(CONTEXT, new McpSchema.CallToolRequest("echo", Map.of())));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, missing.getJsonRpcError().code());
    }

    @Test
    void missingNullableArgumentsAreNull() {
        var optional = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("optional")).callHandler();
        assertEquals("absent", text(optional.apply(CONTEXT, new McpSchema.CallToolRequest("optional", Map.of()))));
        assertEquals("hi", text(optional.apply(CONTEXT, new McpSchema.CallToolRequest("optional", Map.of("text", "hi")))));
    }

    @Test
    void parametersReceivingTheContextAreNotToolArguments() {
        var spec = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("contextual"));
        // an Object parameter is bound to the transport context, so it is not advertised in the input schema
        assertEquals(Set.of("text"), ((Map<?, ?>) spec.tool().inputSchema().get("properties")).keySet());
        assertEquals("hi true", text(spec.callHandler().apply(CONTEXT, new McpSchema.CallToolRequest("contextual", Map.of("text", "hi")))));
    }

    @Test
    void prompts() {
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("greeting", Map.of());
        assertEquals("Hello", prompt(find(promptRegistry.getSyncSpecs(), s -> s.prompt().name().equals("greeting"))
            .promptHandler().apply(SYNC_EXCHANGE, request)));
        assertEquals("Hello", prompt(find(promptRegistry.getAsyncSpecs(), s -> s.prompt().name().equals("greeting"))
            .promptHandler().apply(ASYNC_EXCHANGE, request).block()));
        assertEquals("Hello", prompt(find(promptRegistry.getStatelessSyncSpecs(), s -> s.prompt().name().equals("greeting"))
            .promptHandler().apply(CONTEXT, request)));
        assertEquals("Hello", prompt(find(promptRegistry.getStatelessAsyncSpecs(), s -> s.prompt().name().equals("greeting"))
            .promptHandler().apply(CONTEXT, request).block()));
    }

    @Test
    void resources() {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("handlers://readme");
        assertEquals("readme", resource(find(resourceRegistry.getSyncSpecs(), s -> s.resource().uri().equals("handlers://readme"))
            .readHandler().apply(SYNC_EXCHANGE, request)));
        assertEquals("readme", resource(find(resourceRegistry.getAsyncSpecs(), s -> s.resource().uri().equals("handlers://readme"))
            .readHandler().apply(ASYNC_EXCHANGE, request).block()));
        assertEquals("readme", resource(find(resourceRegistry.getStatelessSyncSpecs(), s -> s.resource().uri().equals("handlers://readme"))
            .readHandler().apply(CONTEXT, request)));
        assertEquals("readme", resource(find(resourceRegistry.getStatelessAsyncSpecs(), s -> s.resource().uri().equals("handlers://readme"))
            .readHandler().apply(CONTEXT, request).block()));
    }

    @Test
    void resourceTemplates() {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("handlers://pages/7");
        String template = "handlers://pages/{page}";
        assertEquals("page 7", resource(find(resourceTemplateRegistry.getSyncSpecs(), s -> s.resourceTemplate().uriTemplate().equals(template))
            .readHandler().apply(SYNC_EXCHANGE, request)));
        assertEquals("page 7", resource(find(resourceTemplateRegistry.getAsyncSpecs(), s -> s.resourceTemplate().uriTemplate().equals(template))
            .readHandler().apply(ASYNC_EXCHANGE, request).block()));
        assertEquals("page 7", resource(find(resourceTemplateRegistry.getStatelessSyncSpecs(), s -> s.resourceTemplate().uriTemplate().equals(template))
            .readHandler().apply(CONTEXT, request)));
        assertEquals("page 7", resource(find(resourceTemplateRegistry.getStatelessAsyncSpecs(), s -> s.resourceTemplate().uriTemplate().equals(template))
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

    private static <T> T find(List<T> specs, Predicate<T> predicate) {
        return specs.stream().filter(predicate).findFirst().orElseThrow();
    }

    private static void assertError(int code, String message, Executable call) {
        McpError error = assertThrows(McpError.class, call);
        assertEquals(code, error.getJsonRpcError().code());
        assertEquals(message, error.getJsonRpcError().message());
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

        @Tool
        String silent() {
            throw new IllegalStateException();
        }

        @Tool
        String anonymous() {
            throw new IllegalStateException() { };
        }

        @Tool
        String invalid() {
            throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("bad").build();
        }

        @Tool
        String optional(@Nullable String text) {
            return text == null ? "absent" : text;
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
