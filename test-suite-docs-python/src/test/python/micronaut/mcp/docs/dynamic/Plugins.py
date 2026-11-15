from micronaut.context.annotation import Requires
# tag::imports[]
from io.modelcontextprotocol.server import McpStatelessServerFeatures, McpStatelessSyncServer
from io.modelcontextprotocol.spec import McpSchema
from jakarta.inject import Singleton
from java.util import Map
# end::imports[]


@Requires(property="spec.name", value="PluginsTest")
# tag::clazz[]
@Singleton
class Plugins:

    def __init__(self, server: McpStatelessSyncServer):
        self.server = server

    def install(self, name: str) -> None:
        self.server.addTool(McpStatelessServerFeatures.SyncToolSpecification.builder()
                            .tool(McpSchema.Tool.builder().name(name).inputSchema(Map.of("type", "object")).build())
                            .callHandler(lambda context, request:
                                         McpSchema.CallToolResult.builder().addTextContent("installed").build())
                            .build())

    def uninstall(self, name: str) -> None:
        self.server.removeTool(name)
# end::clazz[]
