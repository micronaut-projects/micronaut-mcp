package io.micronaut.mcp.docs.context;

import java.util.ArrayList;
import java.util.List;

import io.micronaut.context.annotation.Requires;
//tag::imports[]
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
//end::imports[]

@Requires(property = "spec.name", value = "CatalogToolsTest")
//tag::clazz[]
@Singleton
class CatalogTools {
    @Tool(description = "Imports the pages of the catalog")
    String importCatalog(int pages, McpRequestContext context) {
        for (int page = 1; page <= pages; page++) {
            importPage(page);
            context.progress(page, (double) pages, "Imported page " + page); // <1>
        }
        context.log(McpSchema.LoggingLevel.INFO, "catalog", "Imported " + pages + " pages"); // <2>
        return "done";
    }
//end::clazz[]

    private final List<Integer> importedPages = new ArrayList<>();

    private void importPage(int page) {
        importedPages.add(page);
    }
//tag::clazz[]
}
//end::clazz[]
