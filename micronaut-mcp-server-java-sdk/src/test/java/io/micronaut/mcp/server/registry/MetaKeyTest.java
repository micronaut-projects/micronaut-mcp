package io.micronaut.mcp.server.registry;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Meta;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetaKeyTest {

    @Test
    void anEmptyKeyFailsTheStartup() {
        assertEquals("The @Meta of method " + EmptyKey.class.getName() + "#tool requires a key", startupFailure("MetaKeyTest.empty"));
    }

    @ParameterizedTest
    @CsvSource({"DuplicateKeyTool,tool", "DuplicateKeyPrompt,prompt", "DuplicateKeyResource,resource", "DuplicateKeyTemplate,template"})
    void aKeyDeclaredTwiceFailsTheStartupOnEveryPrimitive(String bean, String method) {
        assertEquals("The @Meta key com.example/kind is declared twice on method " + MetaKeyTest.class.getName() + "$" + bean + "#" + method,
            startupFailure("MetaKeyTest." + bean));
    }

    private static String startupFailure(String specName) {
        Map<String, Object> properties = Map.of(
            "micronaut.mcp.server.info.name", "mcp-server",
            "micronaut.mcp.server.info.version", "0.0.1",
            "micronaut.mcp.server.transport", "HTTP",
            "spec.name", specName);
        Throwable error = assertThrows(RuntimeException.class, () -> ApplicationContext.run(properties));
        while (error.getCause() != null && !(error instanceof IllegalStateException)) {
            error = error.getCause();
        }
        return error.getMessage();
    }

    @Requires(property = "spec.name", value = "MetaKeyTest.empty")
    @Singleton
    static class EmptyKey {
        @Tool
        @Meta(key = "", value = "x")
        String tool() {
            return "tool";
        }
    }

    @Requires(property = "spec.name", value = "MetaKeyTest.DuplicateKeyTool")
    @Singleton
    static class DuplicateKeyTool {
        @Tool
        @Meta(key = "com.example/kind", value = "trip")
        @Meta(key = "com.example/kind", value = "holiday")
        String tool() {
            return "tool";
        }
    }

    @Requires(property = "spec.name", value = "MetaKeyTest.DuplicateKeyPrompt")
    @Singleton
    static class DuplicateKeyPrompt {
        @Prompt
        @Meta(key = "com.example/kind", value = "trip")
        @Meta(key = "com.example/kind", value = "holiday")
        String prompt() {
            return "Plan a trip";
        }
    }

    @Requires(property = "spec.name", value = "MetaKeyTest.DuplicateKeyResource")
    @Singleton
    static class DuplicateKeyResource {
        @Resource(uri = "meta://duplicate")
        @Meta(key = "com.example/kind", value = "trip")
        @Meta(key = "com.example/kind", value = "holiday")
        String resource() {
            return "resource";
        }
    }

    @Requires(property = "spec.name", value = "MetaKeyTest.DuplicateKeyTemplate")
    @Singleton
    static class DuplicateKeyTemplate {
        @ResourceTemplate(uriTemplate = "meta://duplicate/{id}")
        @Meta(key = "com.example/kind", value = "trip")
        @Meta(key = "com.example/kind", value = "holiday")
        String template(String id) {
            return id;
        }
    }
}
