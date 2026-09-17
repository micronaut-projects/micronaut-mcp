from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import Tool


@Requires(property="spec.name", value="HelloWorldToolTest")
# tag::clazz[]
@Singleton
class HelloWorldTool:
    @Tool(title="Hello World",
          annotations=Tool.ToolAnnotations(readOnlyHint=True,
                                           title="Hello World",
                                           destructiveHint=False,
                                           idempotentHint=True,
                                           openWorldHint=False,
                                           returnDirect=True))
    def hello_world(self) -> str:
        return "Hello, World!"
# end::clazz[]
