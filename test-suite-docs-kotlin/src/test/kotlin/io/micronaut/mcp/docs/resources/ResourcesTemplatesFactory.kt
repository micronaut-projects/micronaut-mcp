package io.micronaut.mcp.docs.resources

/*
//tag::fakepackage[]
package example.micronaut

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.docs.resources.ResourcesFactory.Companion.PGN_MIME_TYPE
import io.micronaut.mcp.docs.resources.ResourcesFactory.Companion.readResourceResult
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton

//end::imports[]

@Requires(property = "spec.name", value = "ResourcesFactoryTest")
//tag::clazz[]
@Factory
class ResourcesTemplatesFactory(private val pgnLoader: PgnLoader) {

    @Singleton
    fun pgnResourceTemplateSpecification(): McpStatelessServerFeatures.SyncResourceTemplateSpecification {
        val resourceTemplate = createPgnResourceTemplate()
        return McpStatelessServerFeatures.SyncResourceTemplateSpecification(resourceTemplate) { _, readResourceRequest ->
            readResourceResult(readResourceRequest.uri(), pgnLoader)
        }
    }

    fun createPgnResourceTemplate(): McpSchema.ResourceTemplate {
        val uriTemplate = "pgn://round/{round}"
        val name = "2024ChessChampionshipRoundPgn"
        val title = "PGN of a round World Chess Championship 2024"
        val description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju"
        return McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null)
    }
}
//end::clazz[]
