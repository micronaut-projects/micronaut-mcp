package io.micronaut.mcp.conf.server;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerInfoConfigurationWarningTest {

    @Test
    void warnsWhenServerInfoPropertiesAreSetWithoutNameAndVersion() {
        assertTrue(warns(Map.of("micronaut.mcp.server.info.instructions", "Ask for a city first.")));
        assertTrue(warns(Map.of("micronaut.mcp.server.info.name", "weather", "micronaut.mcp.server.info.title", "Weather")));
    }

    @Test
    void doesNotWarnWhenNameAndVersionAreSet() {
        assertFalse(warns(Map.of("micronaut.mcp.server.info.name", "weather", "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.info.instructions", "Ask for a city first.")));
    }

    @Test
    void doesNotWarnWithoutServerInfoProperties() {
        assertFalse(warns(Map.of()));
    }

    @Test
    void groovyStyleCamelCaseIconKeysBind() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.mcp.server.info.name", "weather",
            "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.info.icons", java.util.List.of(Map.of("src", "https://example.com/weather.png", "mimeType", "image/png"))))) {
            assertEquals("image/png", context.getBean(McpServerInfoConfiguration.class).getIcons().get(0).getMimeType());
        }
    }

    private static boolean warns(Map<String, Object> properties) {
        try (ApplicationContext context = ApplicationContext.run(properties)) {
            return context.containsBean(McpServerInfoConfigurationWarning.class);
        }
    }
}
