package io.micronaut.mcp.docs.dynamic

import io.micronaut.context.annotation.Requires
//tag::imports[]
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.server.McpStatelessSyncServer
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton
//end::imports[]

@Requires(property = "spec.name", value = "PluginsTest")
//tag::clazz[]
@Singleton
class Plugins(private val server: McpStatelessSyncServer) {

    fun install(name: String) {
        server.addTool(McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name(name).inputSchema(mapOf<String, Any>("type" to "object")).build())
            .callHandler { _, _ -> McpSchema.CallToolResult.builder().addTextContent("installed").build() }
            .build())
    }

    fun uninstall(name: String) {
        server.removeTool(name)
    }
}
//end::clazz[]
