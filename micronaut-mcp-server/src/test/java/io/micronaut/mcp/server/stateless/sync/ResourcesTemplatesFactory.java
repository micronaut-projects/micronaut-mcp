package io.micronaut.mcp.server.stateless.sync;

/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import static io.micronaut.mcp.server.stateless.sync.ResourcesFactory.PGN_MIME_TYPE;

//end::imports[]
@Requires(property = "spec.name", value = "StatelessSyncResourceListTest")
//tag::clazz[]
@Factory
class ResourcesTemplatesFactory {
    @Singleton
    McpSchema.ResourceTemplate createPgnResourceTemplate() {
        String uriTemplate = "pgn://round/{round}";
        String name = "2024ChessChampionshipRoundPgn";
        String title = "PGN of a round World Chess Championship 2024";
        String description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju";
        return new McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null);
    }
}
//end::clazz[]
