from micronaut.context.annotation import Requires
# tag::imports[]
from io.modelcontextprotocol.spec import McpSchema
from jakarta.inject import Singleton
from micronaut.mcp.annotations import Tool
from micronaut.mcp.server.context import McpRequestContext
# end::imports[]


def import_page(page: int) -> None:
    pass


@Requires(property="spec.name", value="CatalogToolsTest")
# tag::clazz[]
@Singleton
class CatalogTools:

    @Tool(description="Imports the pages of the catalog")
    def import_catalog(self, pages: int, context: McpRequestContext) -> str:
        for page in range(1, pages + 1):
            import_page(page)
            context.progress(float(page), float(pages), f"Imported page {page}")  # <1>
        context.log(McpSchema.LoggingLevel.INFO, "catalog", f"Imported {pages} pages")  # <2>
        return "done"
# end::clazz[]
