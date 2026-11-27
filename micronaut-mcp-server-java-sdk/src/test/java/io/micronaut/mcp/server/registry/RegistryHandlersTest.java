package io.micronaut.mcp.server.registry;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invokes the handlers of every specification variant the registries build: STDIO sync and async, with server
 * exchanges, and stateless sync and async, with transport contexts.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "RegistryHandlersTest")
@Property(name = "micronaut.executors.blocking.type", value = "fixed")
@Property(name = "micronaut.executors.blocking.n-threads", value = "1")
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

    @Inject
    CompleteRequestArgumentBinderRegistry completeRequestArgumentBinderRegistry;

    @Inject
    BeanContext beanContext;

    @Inject
    Primitives primitives;

    @Inject
    CountingMapper countingMapper;

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
    void stdioSyncDispatchesToTheBlockingExecutor(@Named(TaskExecutors.BLOCKING) ExecutorService executor) throws Exception {
        long worker = executor.submit(() -> Thread.currentThread().threadId()).get(5, TimeUnit.SECONDS);
        var spec = find(toolRegistry.getSyncSpecs(), s -> s.tool().name().equals("blockingWorker"));
        var request = new McpSchema.CallToolRequest("blockingWorker", Map.of());
        assertEquals(Long.toString(worker), text(spec.callHandler().apply(SYNC_EXCHANGE, request)));
    }

    @Test
    void unmappedExceptionsAreToolErrorsWithTheirMessage() {
        var failing = toolHandler("failing");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("failing", Map.of());
        // twice, so the second lookup hits the cached miss
        assertToolError("failing", () -> failing.apply(CONTEXT, request));
        assertToolError("failing", () -> failing.apply(CONTEXT, request));
    }

    @Test
    void exceptionsWithoutAMessageAreReportedWithTheirType() {
        // the model is told which tool failed and how
        assertToolError("Tool silent failed (IllegalStateException)",
            () -> toolHandler("silent").apply(CONTEXT, new McpSchema.CallToolRequest("silent", Map.of())));
        assertToolError("Tool blank failed (IllegalStateException)",
            () -> toolHandler("blank").apply(CONTEXT, new McpSchema.CallToolRequest("blank", Map.of())));
    }

    @Test
    void exceptionsOfAnAnonymousClassWithoutAMessageAreReportedWithTheNameOfTheirClass() {
        McpSchema.CallToolResult result = toolHandler("anonymous").apply(CONTEXT, new McpSchema.CallToolRequest("anonymous", Map.of()));
        assertTrue(result.isError());
        // an anonymous class has no simple name
        assertTrue(text(result).startsWith("Tool anonymous failed (" + Primitives.class.getName() + "$"));
    }

    @Test
    void aDestroyedSingletonIsReplaced() {
        var instance = toolHandler("instance");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("instance", Map.of());
        String first = text(instance.apply(CONTEXT, request));
        assertEquals(first, text(instance.apply(CONTEXT, request)));
        beanContext.destroyBean(Rotating.class);
        assertNotEquals(first, text(instance.apply(CONTEXT, request)));
    }

    @Test
    void anMcpErrorKeepsItsCodeAndMessage() {
        assertError(McpSchema.ErrorCodes.INVALID_PARAMS, "bad",
            () -> toolHandler("invalid").apply(CONTEXT, new McpSchema.CallToolRequest("invalid", Map.of())));
    }

    @Test
    void aRegisteredMapperTakesPrecedenceOverMcpErrorPassthrough() {
        var request = new McpSchema.CallToolRequest("mappedMcpError", Map.of());
        assertError(McpSchema.ErrorCodes.INVALID_REQUEST, "mapped MCP error",
            () -> toolHandler("mappedMcpError").apply(CONTEXT, request));
    }

    @Test
    void mappedExceptionsAreMappedByTheirMapper() {
        assertToolError("Required argument [String text] not specified",
            () -> toolHandler("echo").apply(CONTEXT, new McpSchema.CallToolRequest("echo", Map.of())));
        var mapped = toolHandler("mapped");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("mapped", Map.of());
        assertError(McpSchema.ErrorCodes.INVALID_REQUEST, "mapped", () -> mapped.apply(CONTEXT, request));
        assertError(McpSchema.ErrorCodes.INVALID_REQUEST, "mapped", () -> mapped.apply(CONTEXT, request));
    }

    @Test
    void theMappersAreAskedOncePerExceptionType() {
        McpSchema.CallToolRequest failing = new McpSchema.CallToolRequest("failing", Map.of());
        McpSchema.CallToolRequest mapped = new McpSchema.CallToolRequest("mapped", Map.of());
        for (int i = 0; i < 2; i++) {
            assertTrue(toolHandler("failing").apply(CONTEXT, failing).isError());
            assertThrows(McpError.class, () -> toolHandler("mapped").apply(CONTEXT, mapped));
        }
        assertEquals(1, countingMapper.canMapCalls(IllegalStateException.class));
        assertEquals(1, countingMapper.canMapCalls(MappedException.class));
    }

    @Test
    void jsonSchemaArgumentsAreBoundOnEveryVariantWhateverTheOrderTheSpecificationsAreBuilt() {
        toolRegistry.getStatelessSyncSpecs();
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("area", Map.of("width", 2, "height", 3));
        assertEquals("6", text(toolHandler("area").apply(CONTEXT, request)));
        assertEquals("6", text(find(toolRegistry.getSyncSpecs(), s -> s.tool().name().equals("area")).callHandler().apply(SYNC_EXCHANGE, request)));
        assertEquals("6", text(find(toolRegistry.getStatelessAsyncSpecs(), s -> s.tool().name().equals("area")).callHandler().apply(CONTEXT, request).block()));
        // a value of the wrong type is a missing argument
        assertToolError("Required argument [HandlersRectangle rectangle] not specified",
            () -> toolHandler("area").apply(CONTEXT, new McpSchema.CallToolRequest("area", Map.of("width", "two", "height", 3))));
        // and so is an integer out of the range of an int, which is not narrowed
        assertToolError("Required argument [HandlersRectangle rectangle] not specified",
            () -> toolHandler("area").apply(CONTEXT, new McpSchema.CallToolRequest("area", Map.of("width", 4294967297L, "height", 3))));
    }

    @Test
    void aValueThatCannotBeConvertedIsAMissingArgumentNotAnInternalError() {
        // the argument binders report conversion errors in their result, so no ConversionErrorException reaches the client
        assertToolError("Required argument [Integer b] not specified",
            () -> toolHandler("mixed").apply(CONTEXT, new McpSchema.CallToolRequest("mixed", Map.of("a", "x", "b", "abc"))));
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
    void boundParametersAreLeftOutOfTheInputSchemaWhereverTheyAreDeclared() {
        var spec = find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals("mixed"));
        assertEquals(Set.of("a", "b"), ((Map<?, ?>) spec.tool().inputSchema().get("properties")).keySet());
        assertEquals(List.of("a", "b"), spec.tool().inputSchema().get("required"));
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("mixed", Map.of("a", "x", "b", 1));
        assertEquals("true x mixed 1", text(spec.callHandler().apply(CONTEXT, request)));
    }

    @Test
    void promptsReceiveTheRequestAndTheTransportContext() {
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("introduce", Map.of());
        find(promptRegistry.getSyncSpecs(), s -> s.prompt().name().equals("introduce")).promptHandler().apply(SYNC_EXCHANGE, request);
        assertReceived(request, CONTEXT);
        find(promptRegistry.getAsyncSpecs(), s -> s.prompt().name().equals("introduce")).promptHandler().apply(ASYNC_EXCHANGE, request).block();
        assertReceived(request, CONTEXT);
        find(promptRegistry.getStatelessSyncSpecs(), s -> s.prompt().name().equals("introduce")).promptHandler().apply(CONTEXT, request);
        assertReceived(request, CONTEXT);
        find(promptRegistry.getStatelessAsyncSpecs(), s -> s.prompt().name().equals("introduce")).promptHandler().apply(CONTEXT, request).block();
        assertReceived(request, CONTEXT);
    }

    @Test
    void resourcesReceiveTheRequestAndTheTransportContext() {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("handlers://request");
        find(resourceRegistry.getSyncSpecs(), s -> s.resource().uri().equals("handlers://request")).readHandler().apply(SYNC_EXCHANGE, request);
        assertReceived(request, CONTEXT);
        find(resourceRegistry.getStatelessSyncSpecs(), s -> s.resource().uri().equals("handlers://request")).readHandler().apply(CONTEXT, request);
        assertReceived(request, CONTEXT);
    }

    @Test
    void resourceTemplatesReceiveTheRequest() {
        String template = "handlers://requests/{id}";
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("handlers://requests/1");
        find(resourceTemplateRegistry.getSyncSpecs(), s -> s.resourceTemplate().uriTemplate().equals(template)).readHandler().apply(SYNC_EXCHANGE, request);
        assertReceived(request, CONTEXT);
        find(resourceTemplateRegistry.getStatelessSyncSpecs(), s -> s.resourceTemplate().uriTemplate().equals(template)).readHandler().apply(CONTEXT, request);
        assertReceived(request, CONTEXT);
    }

    @Test
    void aResourceTemplateHandlerMatchesEveryRequestAgainstItsTemplate() {
        var handler = find(resourceTemplateRegistry.getStatelessSyncSpecs(), s -> s.resourceTemplate().uriTemplate().equals("handlers://pages/{page}")).readHandler();
        assertEquals("page 7", resource(handler.apply(CONTEXT, new McpSchema.ReadResourceRequest("handlers://pages/7"))));
        assertEquals("page 8", resource(handler.apply(CONTEXT, new McpSchema.ReadResourceRequest("handlers://pages/8"))));
        // a URI that does not match the template has no variables, so the page is missing, which is invalid params
        McpError error = assertThrows(McpError.class, () -> handler.apply(CONTEXT, new McpSchema.ReadResourceRequest("handlers://other/7")));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
    }

    @Test
    void registriesWithMethodsAreNotEmpty() {
        assertTrue(toolRegistry.isNotEmpty());
        assertTrue(promptRegistry.isNotEmpty());
        assertTrue(resourceRegistry.isNotEmpty());
        assertTrue(resourceTemplateRegistry.isNotEmpty());
        assertTrue(completionRegistry.isNotEmpty());
        assertFalse(new CompletionRegistry(List.of(), beanContext, completeRequestArgumentBinderRegistry).isNotEmpty());
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
        assertSame(request, primitives.request);
        assertSame(request.argument(), primitives.argument);
    }

    private McpStatelessServerFeatures.SyncToolSpecification toolSpec(String name) {
        return find(toolRegistry.getStatelessSyncSpecs(), s -> s.tool().name().equals(name));
    }

    private BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult> toolHandler(String name) {
        return toolSpec(name).callHandler();
    }

    private void assertReceived(Object request, McpTransportContext context) {
        assertSame(request, primitives.request);
        assertSame(context, primitives.context);
    }

    private static <T> T find(List<T> specs, Predicate<T> predicate) {
        return specs.stream().filter(predicate).findFirst().orElseThrow();
    }

    private static void assertToolError(String message, Supplier<McpSchema.CallToolResult> call) {
        McpSchema.CallToolResult result = call.get();
        assertTrue(result.isError());
        assertEquals(message, text(result));
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
        volatile Object request;
        volatile Object context;
        volatile Object argument;

        @Tool
        @ExecuteOn(TaskExecutors.BLOCKING)
        String blockingWorker() {
            return Long.toString(Thread.currentThread().threadId());
        }

        @Tool
        String echo(String text) {
            return text;
        }

        @Tool
        String mixed(Object context, String a, McpSchema.CallToolRequest request, Integer b) {
            return (context instanceof McpTransportContext) + " " + a + " " + request.name() + " " + b;
        }

        @Tool
        String area(HandlersRectangle rectangle) {
            return String.valueOf(rectangle.width() * rectangle.height());
        }

        @Tool
        String blank() {
            throw new IllegalStateException("  ");
        }

        @Tool
        String mapped() {
            throw new MappedException("mapped");
        }

        @Prompt
        String introduce(McpSchema.GetPromptRequest request, McpTransportContext context) {
            this.request = request;
            this.context = context;
            return "Hello";
        }

        @Resource(uri = "handlers://request")
        String requestResource(McpSchema.ReadResourceRequest request, McpTransportContext context) {
            this.request = request;
            this.context = context;
            return "request";
        }

        @ResourceTemplate(uriTemplate = "handlers://requests/{id}")
        String requestTemplate(String id, McpSchema.ReadResourceRequest request, McpTransportContext context) {
            this.request = request;
            this.context = context;
            return "request " + id;
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
        String mappedMcpError() {
            throw new MappedMcpError("mapped MCP error");
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
        List<String> greetingCompletion(McpSchema.CompleteRequest request, McpSchema.CompleteRequest.CompleteArgument argument) {
            this.request = request;
            this.argument = argument;
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

    @JsonSchema
    @Serdeable
    record HandlersRectangle(int width, int height) {
    }

    static class MappedException extends RuntimeException {
        MappedException(String message) {
            super(message);
        }
    }

    static class MappedMcpError extends McpError {
        MappedMcpError(String message) {
            super(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, message));
        }
    }

    @Requires(property = "spec.name", value = "RegistryHandlersTest")
    @Singleton
    static class CountingMapper implements McpErrorExceptionMapper<RuntimeException> {
        private final Map<Class<?>, AtomicInteger> canMapCalls = new ConcurrentHashMap<>();

        @Override
        public boolean canMap(Class<? extends Throwable> clazz) {
            canMapCalls.computeIfAbsent(clazz, c -> new AtomicInteger()).incrementAndGet();
            return MappedException.class.isAssignableFrom(clazz) || MappedMcpError.class.isAssignableFrom(clazz);
        }

        @Override
        public McpError map(RuntimeException exception) {
            return McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST).message(exception.getMessage()).build();
        }

        int canMapCalls(Class<?> clazz) {
            AtomicInteger calls = canMapCalls.get(clazz);
            return calls == null ? 0 : calls.get();
        }
    }

    @Requires(property = "spec.name", value = "RegistryHandlersTest")
    @Singleton
    static class Rotating {
        private static final AtomicInteger INSTANCES = new AtomicInteger();
        private final int instance = INSTANCES.incrementAndGet();

        @Tool
        String instance() {
            return String.valueOf(instance);
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
