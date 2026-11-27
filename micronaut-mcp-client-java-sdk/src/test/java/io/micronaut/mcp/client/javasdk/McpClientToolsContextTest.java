package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.propagation.PropagatedContextElement;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The tools of every connection are listed in the Micronaut context of the caller, such as the HTTP request whose
 * credentials the headers providers forward.
 */
class McpClientToolsContextTest {

    record Caller(String name) implements PropagatedContextElement {
    }

    @Test
    void theToolsOfEveryConnectionAreListedInTheContextOfTheCaller() {
        try (ApplicationContext context = ApplicationContext.run()) {
            McpClientTools callerTools = () -> PropagatedContext.getOrEmpty().find(Caller.class)
                .map(caller -> List.of(new McpClientTool(caller.name(), "weather", McpSchema.Tool.builder().name(caller.name()).build(), null)))
                .orElse(List.of());
            context.registerSingleton(McpClientTools.class, callerTools, Qualifiers.byName("weather"));
            McpClientToolsFactory factory = new McpClientToolsFactory(context, context.getBean(McpConnectionClients.class), null, null, List.of());
            McpClientConnectionConfiguration weather = () -> "weather";
            McpClientTools all = factory.allTools(List.of(weather));
            List<McpClientTool> tools = PropagatedContext.empty().plus(new Caller("alice")).propagate(all::tools);
            assertEquals(List.of("alice"), tools.stream().map(McpClientTool::name).toList());
        }
    }
}
