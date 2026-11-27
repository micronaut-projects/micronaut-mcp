from micronaut.context.annotation import Requires
# tag::imports[]
from jakarta.inject import Singleton
from micronaut.mcp.annotations import Tool
from micronaut.mcp.server.context import MicronautMcpTransportContext
# end::imports[]


@Requires(property="spec.name", value="WhoAmIToolsTest")
# tag::clazz[]
@Singleton
class WhoAmITools:

    @Tool(description="Says who the authenticated user is")
    def whoami(self, context: MicronautMcpTransportContext) -> str:
        principal = context.principal()
        return principal.getName() if principal is not None else "anonymous"
# end::clazz[]
