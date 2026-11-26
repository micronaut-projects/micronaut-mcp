package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.observability.McpServerObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Invokes handlers with requests that name the primitive differently from its declaration, as an SDK that matched
 * loosely would: observations still use the declared name, so requests cannot add names.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.metrics.enabled", value = "false")
@Property(name = "spec.name", value = "DeclaredNameObservationTest")
@MicronautTest(startApplication = false)
class DeclaredNameObservationTest {
    private static final McpTransportContext CONTEXT = McpTransportContext.create(Map.of());

    @Inject
    ToolRegistry toolRegistry;

    @Inject
    PromptRegistry promptRegistry;

    @Inject
    ResourceRegistry resourceRegistry;

    @Inject
    CompletionRegistry completionRegistry;

    @Inject
    RecordingObserver observer;

    @Test
    void operationsAreObservedWithTheDeclaredNames() {
        toolRegistry.getStatelessSyncSpecs().getFirst().callHandler()
            .apply(CONTEXT, new McpSchema.CallToolRequest("ECHO", Map.of()));
        promptRegistry.getStatelessSyncSpecs().stream().filter(s -> s.prompt().name().equals("greet")).findFirst().orElseThrow()
            .promptHandler().apply(CONTEXT, new McpSchema.GetPromptRequest("GREET", Map.of("name", "Bob")));
        resourceRegistry.getStatelessSyncSpecs().getFirst().readHandler()
            .apply(CONTEXT, new McpSchema.ReadResourceRequest("declared://README"));
        completionRegistry.getStatelessSyncSpecs().getFirst().completionHandler()
            .apply(CONTEXT, new McpSchema.CompleteRequest(new McpSchema.PromptReference("GREET"), new McpSchema.CompleteRequest.CompleteArgument("name", "a")));

        assertEquals(List.of("tools/call echo", "prompts/get greet", "resources/read declared://readme", "completion/complete greet"),
            observer.names);
    }

    @Requires(property = "spec.name", value = "DeclaredNameObservationTest")
    @Singleton
    static class Primitives {
        @Tool
        String echo() {
            return "echo";
        }

        @Prompt
        String greet(String name) {
            return "Hello " + name;
        }

        @PromptCompletion(name = "greet")
        List<String> names() {
            return List.of("Alice");
        }

        @Resource(uri = "declared://readme")
        String readme() {
            return "readme";
        }
    }

    @Requires(property = "spec.name", value = "DeclaredNameObservationTest")
    @Singleton
    static class RecordingObserver implements McpServerObserver {
        final List<String> names = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Observation start(String method, String name) {
            names.add(method + " " + name);
            return Observation.NOOP;
        }
    }
}
