package io.micronaut.mcp.server;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceCompletion;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each kind of primitive that declares an {@link McpRequestContext} may send log messages, so it alone makes the server
 * advertise the logging capability; the same primitives without the context do not.
 */
class LoggingCapabilityPerPrimitiveTest {
    private static final String KIND = "logging-capability-kind";

    @ParameterizedTest
    @ValueSource(strings = {"tool", "prompt", "resource", "template", "prompt-completion", "resource-completion", "none"})
    void eachKindOfPrimitiveWithARequestContextAdvertisesLogging(String kind) {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.server.info.name", "mcp-server",
            "micronaut.mcp.server.info.version", "0.0.1",
            "micronaut.mcp.server.transport", "HTTP",
            KIND, kind))) {
            McpSchema.ServerCapabilities capabilities = context.getBean(McpSchema.ServerCapabilities.class);
            assertEquals(!"none".equals(kind), capabilities.logging() != null, kind);
        }
    }

    // Primitives of every kind without the context, so that only the one with it makes the difference
    @Requires(property = KIND)
    @Singleton
    static class Quiet {
        @Tool
        String quietTool() {
            return "quiet";
        }

        @Prompt
        String quietPrompt() {
            return "quiet";
        }

        @Resource(uri = "quiet://resource")
        String quietResource() {
            return "quiet";
        }

        @ResourceTemplate(uriTemplate = "quiet://items/{id}")
        String quietTemplate(String id) {
            return id;
        }

        @PromptCompletion(name = "quietPrompt")
        List<String> quietPromptCompletion() {
            return List.of();
        }

        @ResourceCompletion(uri = "quiet://items/{id}")
        List<String> quietResourceCompletion() {
            return List.of();
        }
    }

    @Requires(property = KIND, value = "tool")
    @Singleton
    static class NotifyingTool {
        @Tool
        String notifyingTool(McpRequestContext context) {
            return "tool";
        }
    }

    @Requires(property = KIND, value = "prompt")
    @Singleton
    static class NotifyingPrompt {
        @Prompt
        String notifyingPrompt(McpRequestContext context) {
            return "prompt";
        }
    }

    @Requires(property = KIND, value = "resource")
    @Singleton
    static class NotifyingResource {
        @Resource(uri = "notifying://resource")
        String notifyingResource(McpRequestContext context) {
            return "resource";
        }
    }

    @Requires(property = KIND, value = "template")
    @Singleton
    static class NotifyingTemplate {
        @ResourceTemplate(uriTemplate = "notifying://items/{id}")
        String notifyingTemplate(String id, McpRequestContext context) {
            return id;
        }
    }

    @Requires(property = KIND, value = "prompt-completion")
    @Singleton
    static class NotifyingPromptCompletion {
        @PromptCompletion(name = "quietPrompt")
        List<String> notifyingPromptCompletion(McpRequestContext context) {
            return List.of();
        }
    }

    @Requires(property = KIND, value = "resource-completion")
    @Singleton
    static class NotifyingResourceCompletion {
        @ResourceCompletion(uri = "quiet://items/{id}")
        List<String> notifyingResourceCompletion(McpRequestContext context) {
            return List.of();
        }
    }
}
