package io.micronaut.mcp.docs.context

import io.micronaut.context.annotation.Requires
//tag::imports[]
import io.micronaut.mcp.annotations.Tool
import io.micronaut.mcp.server.context.McpRequestContext
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton
//end::imports[]

@Requires(property = "spec.name", value = "CatalogToolsTest")
//tag::clazz[]
@Singleton
class CatalogTools {
    @Tool(description = "Imports the pages of the catalog")
    fun importCatalog(pages: Int, context: McpRequestContext): String {
        for (page in 1..pages) {
            importPage(page)
            context.progress(page.toDouble(), pages.toDouble(), "Imported page $page") // <1>
        }
        context.log(McpSchema.LoggingLevel.INFO, "catalog", "Imported $pages pages") // <2>
        return "done"
    }
//end::clazz[]

    private val importedPages = mutableListOf<Int>()

    private fun importPage(page: Int) {
        importedPages.add(page)
    }
//tag::clazz[]
}
//end::clazz[]
