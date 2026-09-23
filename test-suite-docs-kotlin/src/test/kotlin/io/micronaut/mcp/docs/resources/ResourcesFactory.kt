package io.micronaut.mcp.docs.resources

/*
//tag::fakepackage[]
package example.micronaut

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.io.ResourceLoader
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton
import java.util.Optional

//end::imports[]
@Requires(property = "spec.name", value = "ResourcesFactoryTest")
//tag::clazz[]
@Context
@Factory
class ResourcesFactory(private val resourceLoader: ResourceLoader, private val pgnLoader: PgnLoader) {

    @EachBean(PgnFile::class)
    @Singleton
    fun createPgnSyncResourceSpecification(pgnFile: PgnFile): McpStatelessServerFeatures.SyncResourceSpecification {
        val resource = getResource(pgnFile)
        return McpStatelessServerFeatures.SyncResourceSpecification(resource) { _, readResourceRequest ->
            readResourceResult(readResourceRequest.uri(), pgnLoader)
        }
    }

    private fun getResource(pgnFile: PgnFile): McpSchema.Resource {
        return size(pgnFile.path!!)
            .map { size ->
                val round = pgnFile.round
                val uri = "pgn://round/$round"
                val name = "round${round}PgnFideWCC2024"
                val title = "PGN of the Round $round game of the World Chess Championship"
                val description = "$title between Ding Liren and Gukesh Dommaraju"
                McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null)
            }.orElseThrow { ConfigurationException("unable find resource for path " + pgnFile.path) }
    }

    private fun size(path: String): Optional<Long> {
        val inputStreamOptional = resourceLoader.getResourceAsStream(path)
        if (inputStreamOptional.isPresent) {
            inputStreamOptional.get().use { inputStream ->
                return Optional.of(inputStream.readAllBytes().size.toLong())
            }
        }
        return Optional.empty()
    }

    companion object {
        const val PGN_MIME_TYPE = "application/x-chess-pgn"

        private fun round(uri: String): Int {
            val lastSlash = uri.lastIndexOf('/')
            return uri.substring(lastSlash + 1).toInt()
        }

        fun readResourceResult(uri: String, pgnLoader: PgnLoader): McpSchema.ReadResourceResult {
            val round = round(uri)
            val contents = ArrayList<McpSchema.ResourceContents>()
            pgnLoader.loadPgn(round).ifPresent { text ->
                contents.add(McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text))
            }
            return McpSchema.ReadResourceResult(contents)
        }
    }
}
//end::clazz[]
