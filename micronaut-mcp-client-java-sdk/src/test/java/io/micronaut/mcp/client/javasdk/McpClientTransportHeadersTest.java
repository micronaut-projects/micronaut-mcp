package io.micronaut.mcp.client.javasdk;

import io.modelcontextprotocol.common.McpTransportContext;
import org.junit.jupiter.api.Test;
import reactor.util.context.Context;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpClientTransportHeadersTest {

    @Test
    void theHeadersAreAddedToTheTransportContextAlreadyInTheReactorContext() {
        Context context = Context.of(McpTransportContext.KEY, McpTransportContext.create(Map.of("tenant", "acme")));
        McpTransportContext transportContext = McpClientTransportHeaders.withHeaders(context, Map.of("Authorization", "Bearer caller"))
            .get(McpTransportContext.KEY);
        assertEquals("acme", transportContext.get("tenant"));
        assertEquals(Map.of("Authorization", "Bearer caller"), transportContext.get(McpClientTransportHeaders.HEADERS));
    }

    @Test
    void withoutATransportContextOneIsCreated() {
        McpTransportContext transportContext = McpClientTransportHeaders.withHeaders(Context.empty(), Map.of("Authorization", "Bearer caller"))
            .get(McpTransportContext.KEY);
        assertEquals(Map.of("Authorization", "Bearer caller"), transportContext.get(McpClientTransportHeaders.HEADERS));
    }
}
