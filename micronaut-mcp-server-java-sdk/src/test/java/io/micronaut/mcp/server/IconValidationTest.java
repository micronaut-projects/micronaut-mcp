package io.micronaut.mcp.server;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Icon;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

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

    @Test
    void aConfiguredIconWithAnUnknownThemeFailsTheStartup() {
        assertEquals("The icon micronaut.mcp.server.info.icons[0] has the theme purple, but it must be light or dark",
            startupFailure(Map.of(
                "micronaut.mcp.server.info.icons[0].src", "https://example.com/weather.png",
                "micronaut.mcp.server.info.icons[0].theme", "purple")));
    }

    @Test
    void anIconAnnotationWithAnEmptySrcFailsTheStartup() {
        assertEquals("The @Icon of method " + EmptySrc.class.getName() + "#tool requires a src",
            startupFailure(Map.of("spec.name", "IconValidationTest.src")));
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
        properties.put("micronaut.mcp.server.transport", "HTTP");
        Throwable error = assertThrows(RuntimeException.class, () -> ApplicationContext.run(properties));
        while (error.getCause() != null && !(error instanceof IllegalStateException)) {
            error = error.getCause();
        }
        return error.getMessage();
    }

    @Requires(property = "spec.name", value = "IconValidationTest.src")
    @Singleton
    static class EmptySrc {
        @Tool
        @Icon(src = "")
        String tool() {
            return "tool";
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
