package io.micronaut.mcp.server.stateless;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.util.StringUtils;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each {@code list-changed} property advertises its own capability on a server without primitives, and nothing else
 * does: {@link McpSchema.ServerCapabilities} carries the configured flags, and the stateless server turns them off
 * without adding the capabilities the server does not have.
 */
class ListChangedCapabilitiesTest {

    private static final McpSchema.ServerCapabilities.ToolCapabilities TOOLS = new McpSchema.ServerCapabilities.ToolCapabilities(true);
    private static final McpSchema.ServerCapabilities.PromptCapabilities PROMPTS = new McpSchema.ServerCapabilities.PromptCapabilities(true);
    private static final McpSchema.ServerCapabilities.ResourceCapabilities RESOURCES = new McpSchema.ServerCapabilities.ResourceCapabilities(false, true);

    static Stream<Arguments> configurations() {
        return Stream.of(
            Arguments.of(Map.of(), new McpSchema.ServerCapabilities(null, null, null, null, null, null)),
            Arguments.of(Map.of("micronaut.mcp.server.resources.subscribe", StringUtils.TRUE),
                new McpSchema.ServerCapabilities(null, null, null, null, null, null)),
            Arguments.of(Map.of("micronaut.mcp.server.tools.list-changed", StringUtils.TRUE),
                new McpSchema.ServerCapabilities(null, null, null, null, null, TOOLS)),
            Arguments.of(Map.of("micronaut.mcp.server.prompts.list-changed", StringUtils.TRUE),
                new McpSchema.ServerCapabilities(null, null, null, PROMPTS, null, null)),
            Arguments.of(Map.of("micronaut.mcp.server.resources.list-changed", StringUtils.TRUE),
                new McpSchema.ServerCapabilities(null, null, null, null, RESOURCES, null))
        );
    }

    @ParameterizedTest
    @MethodSource("configurations")
    void eachListChangedPropertyAdvertisesOnlyItsCapability(Map<String, Object> properties, McpSchema.ServerCapabilities expected) {
        Map<String, Object> configuration = new HashMap<>(properties);
        configuration.put("micronaut.mcp.server.info.name", "mcp-server");
        configuration.put("micronaut.mcp.server.info.version", "0.0.1");
        configuration.put("micronaut.mcp.server.transport", "HTTP");
        try (ApplicationContext context = ApplicationContext.run(configuration)) {
            assertEquals(expected, context.getBean(McpSchema.ServerCapabilities.class));
            assertEquals(withoutNotifications(expected), context.getBean(McpStatelessSyncServer.class).getServerCapabilities());
        }
    }

    private static McpSchema.ServerCapabilities withoutNotifications(McpSchema.ServerCapabilities capabilities) {
        return new McpSchema.ServerCapabilities(null, null, null,
            capabilities.prompts() == null ? null : new McpSchema.ServerCapabilities.PromptCapabilities(false),
            capabilities.resources() == null ? null : new McpSchema.ServerCapabilities.ResourceCapabilities(false, false),
            capabilities.tools() == null ? null : new McpSchema.ServerCapabilities.ToolCapabilities(false));
    }
}
