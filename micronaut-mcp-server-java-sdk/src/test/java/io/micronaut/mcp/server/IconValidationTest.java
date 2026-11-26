package io.micronaut.mcp.server;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Icon;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IconValidationTest {

    @Test
    void aConfiguredIconWithoutSrcFailsTheStartup() {
        assertEquals("The icon micronaut.mcp.server.info.icons[1] requires a src",
            startupFailure(Map.of(
                "micronaut.mcp.server.info.icons[0].src", "https://example.com/weather.png",
                "micronaut.mcp.server.info.icons[1].mime-type", "image/png")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HTTP", "STDIO"})
    void aConfiguredIconWithoutSrcFailsTheStartupWithEveryTransport(String transport) {
        assertEquals("The icon micronaut.mcp.server.info.icons[0] requires a src",
            startupFailure(Map.of("micronaut.mcp.server.transport", transport, "micronaut.mcp.server.info.icons[0].theme", "dark")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HTTP", "STDIO"})
    void anIconAnnotationWithAnEmptySrcFailsTheStartupWithEveryTransport(String transport) {
        assertEquals("The @Icon of method " + EmptySrcTool.class.getName() + "#tool requires a src",
            startupFailure(Map.of("micronaut.mcp.server.transport", transport, "spec.name", "IconValidationTest.EmptySrcTool")));
    }

    @Test
    void aConfiguredIconWithAJavascriptSrcFailsTheStartup() {
        assertEquals("The icon micronaut.mcp.server.info.icons[0] has the src javascript:alert(1), but it must be an https or http URL, or a data: URI",
            startupFailure(Map.of("micronaut.mcp.server.info.icons[0].src", "javascript:alert(1)")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///etc/passwd", "example.com"})
    void aWebsiteUrlThatIsNotAnHttpUrlFailsTheStartup(String websiteUrl) {
        assertEquals("The micronaut.mcp.server.info.website-url " + websiteUrl + " must be an https or http URL",
            startupFailure(Map.of("micronaut.mcp.server.info.website-url", websiteUrl)));
    }

    @Test
    void aConfiguredIconWithAnUnknownThemeFailsTheStartup() {
        assertEquals("The icon micronaut.mcp.server.info.icons[0] has the theme purple, but it must be light or dark",
            startupFailure(Map.of(
                "micronaut.mcp.server.info.icons[0].src", "https://example.com/weather.png",
                "micronaut.mcp.server.info.icons[0].theme", "purple")));
    }

    @ParameterizedTest
    @CsvSource({"EmptySrcTool,tool", "EmptySrcPrompt,prompt", "EmptySrcResource,resource", "EmptySrcTemplate,template"})
    void anIconAnnotationWithAnEmptySrcFailsTheStartupOnEveryPrimitive(String bean, String method) {
        assertEquals("The @Icon of method " + IconValidationTest.class.getName() + "$" + bean + "#" + method + " requires a src",
            startupFailure(Map.of("spec.name", "IconValidationTest." + bean)));
    }

    @Test
    void anIconAnnotationWithAnUnknownThemeFailsTheStartup() {
        assertEquals("The @Icon of method " + UnknownTheme.class.getName() + "#tool has the theme purple, but it must be light or dark",
            startupFailure(Map.of("spec.name", "IconValidationTest.theme")));
    }

    private static String startupFailure(Map<String, Object> extraProperties) {
        Map<String, Object> properties = new HashMap<>(extraProperties);
        properties.put("micronaut.mcp.server.info.name", "mcp-server");
        properties.put("micronaut.mcp.server.info.version", "0.0.1");
        properties.putIfAbsent("micronaut.mcp.server.transport", "HTTP");
        Throwable error = assertThrows(RuntimeException.class, () -> ApplicationContext.run(properties));
        while (error.getCause() != null && !(error instanceof IllegalStateException)) {
            error = error.getCause();
        }
        return error.getMessage();
    }

    @Requires(property = "spec.name", value = "IconValidationTest.EmptySrcTool")
    @Singleton
    static class EmptySrcTool {
        @Tool
        @Icon(src = "")
        String tool() {
            return "tool";
        }
    }

    @Requires(property = "spec.name", value = "IconValidationTest.EmptySrcPrompt")
    @Singleton
    static class EmptySrcPrompt {
        @Prompt
        @Icon(src = "")
        String prompt() {
            return "prompt";
        }
    }

    @Requires(property = "spec.name", value = "IconValidationTest.EmptySrcResource")
    @Singleton
    static class EmptySrcResource {
        @Resource(uri = "icon://empty")
        @Icon(src = "")
        String resource() {
            return "resource";
        }
    }

    @Requires(property = "spec.name", value = "IconValidationTest.EmptySrcTemplate")
    @Singleton
    static class EmptySrcTemplate {
        @ResourceTemplate(uriTemplate = "icon://empty/{id}")
        @Icon(src = "")
        String template(String id) {
            return id;
        }
    }

    @Requires(property = "spec.name", value = "IconValidationTest.theme")
    @Singleton
    static class UnknownTheme {
        @Tool
        @Icon(src = "https://example.com/sun.svg", theme = "purple")
        String tool() {
            return "tool";
        }
    }
}
