package io.micronaut.mcp.docs.dynamic

import io.micronaut.context.annotation.Requires
//tag::imports[]
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.server.McpStatelessSyncServer
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton
//end::imports[]

@Requires(property = "spec.name", value = "PluginsSpec")
//tag::clazz[]
@Singleton
class Plugins {
    private final McpStatelessSyncServer server

    Plugins(McpStatelessSyncServer server) {
        this.server = server
    }

    void install(String name) {
        server.addTool(McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name(name).inputSchema([type: 'object'] as Map<String, Object>).build())
            .callHandler { context, request -> McpSchema.CallToolResult.builder().addTextContent("installed").build() }
            .build())
    }

    void uninstall(String name) {
        server.removeTool(name)
    }
}
//end::clazz[]
