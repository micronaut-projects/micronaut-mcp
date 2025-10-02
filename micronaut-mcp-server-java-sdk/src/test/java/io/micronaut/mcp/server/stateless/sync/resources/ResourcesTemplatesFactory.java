package io.micronaut.mcp.server.stateless.sync.resources;

/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import static io.micronaut.mcp.server.stateless.sync.resources.ResourcesFactory.PGN_MIME_TYPE;
import static io.micronaut.mcp.server.stateless.sync.resources.ResourcesFactory.readResourceResult;

//end::imports[]

//tag::clazz[]
@Factory
class ResourcesTemplatesFactory {
    private final PgnLoader pgnLoader;
    ResourcesTemplatesFactory( PgnLoader pgnLoader) {
        this.pgnLoader = pgnLoader;
    }

    @Singleton
    McpStatelessServerFeatures.SyncResourceTemplateSpecification pgnResourceTemplateSpecification() {
        McpSchema.ResourceTemplate resourceTemplate = createPgnResourceTemplate();
        return new McpStatelessServerFeatures.SyncResourceTemplateSpecification(resourceTemplate,
            (mcpTransportContext, readResourceRequest) -> readResourceResult(readResourceRequest.uri(), pgnLoader));
    }

    McpSchema.ResourceTemplate createPgnResourceTemplate() {
        String uriTemplate = "pgn://round/{round}";
        String name = "2024ChessChampionshipRoundPgn";
        String title = "PGN of a round World Chess Championship 2024";
        String description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju";
        return new McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null);
    }
}
//end::clazz[]
