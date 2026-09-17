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
import io.modelcontextprotocol.common.McpTransportContext
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "ResourcesFactorySpec")
//tag::clazz[]
@Context
@Factory
class ResourcesFactory {
    public static final String PGN_MIME_TYPE = "application/x-chess-pgn"

    private final PgnLoader pgnLoader
    private final ResourceLoader resourceLoader

    ResourcesFactory(ResourceLoader resourceLoader, PgnLoader pgnLoader) {
        this.resourceLoader = resourceLoader
        this.pgnLoader = pgnLoader
    }

    @EachBean(PgnFile)
    @Singleton
    McpStatelessServerFeatures.SyncResourceSpecification createPgnSyncResourceSpecification(PgnFile pgnFile) throws IOException {
        McpSchema.Resource resource = getResource(pgnFile)
        new McpStatelessServerFeatures.SyncResourceSpecification(resource,
            { McpTransportContext ctx, McpSchema.ReadResourceRequest readResourceRequest -> readResourceResult(readResourceRequest.uri(), pgnLoader) })
    }

    private static Integer round(String uri) {
        int lastSlash = uri.lastIndexOf('/')
        String roundStr = uri.substring(lastSlash + 1)
        Integer.parseInt(roundStr)
    }

    static McpSchema.ReadResourceResult readResourceResult(String uri, PgnLoader pgnLoader) {
        Integer round = round(uri)
        List<McpSchema.ResourceContents> contents = []
        pgnLoader.loadPgn(round).ifPresent { String text ->
            contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text))
        }
        new McpSchema.ReadResourceResult(contents)
    }

    private McpSchema.Resource getResource(PgnFile pgnFile) throws IOException {
        size(pgnFile.path)
            .map { Long size ->
                Integer round = pgnFile.round
                String uri = "pgn://round/${round}"
                String name = "round${round}PgnFideWCC2024"
                String title = "PGN of the Round ${round} game of the World Chess Championship"
                String description = "${title} between Ding Liren and Gukesh Dommaraju"
                new McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null)
        }.orElseThrow { new ConfigurationException("unable find resource for path " + pgnFile.path) }
    }

    private Optional<Long> size(String path) throws IOException {
        Optional<InputStream> inputStreamOptional = resourceLoader.getResourceAsStream(path)
        if (inputStreamOptional.isPresent()) {
            return inputStreamOptional.get().withCloseable { InputStream inputStream ->
                Optional.of((long) inputStream.readAllBytes().length)
            }
        }
        Optional.empty()
    }
}
//end::clazz[]
