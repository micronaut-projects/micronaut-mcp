package io.micronaut.mcp.server.stateless.sync.resources;

/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//end::imports[]
@Requires(property = "spec.name", value = "StatelessSyncResourceListTest")
//tag::clazz[]
@Context
@Factory
class ResourcesFactory {
    public static final String PGN_MIME_TYPE = "application/x-chess-pgn";

    private final PgnLoader pgnLoader;
    private final ResourceLoader resourceLoader;

    ResourcesFactory(ResourceLoader resourceLoader, PgnLoader pgnLoader) {
        this.resourceLoader = resourceLoader;
        this.pgnLoader = pgnLoader;
    }

    @EachBean(PgnFile.class)
    @Singleton
    McpStatelessServerFeatures.SyncResourceSpecification createPgnSyncResourceSpecification(PgnFile pgnFile) {
        McpSchema.Resource resource = getResource(pgnFile);
        return new McpStatelessServerFeatures.SyncResourceSpecification(resource,
            (mcpSyncServerExchange,  readResourceRequest) -> readResourceResult(readResourceRequest.uri(), pgnLoader));
    }

    private static Integer round(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        String roundStr = uri.substring(lastSlash + 1);
        return Integer.parseInt(roundStr);
    }

    static McpSchema.ReadResourceResult readResourceResult(String uri, PgnLoader pgnLoader) {
        Integer round = round(uri);
        List<McpSchema.ResourceContents> contents = new ArrayList<>();
        pgnLoader.loadPgn(round).ifPresent(text ->
            contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text)));
        return new McpSchema.ReadResourceResult(contents);
    }

    private McpSchema.Resource getResource(PgnFile pgnFile) {
        Optional<InputStream> roundPgnInputStreamOptional = resourceLoader.getResourceAsStream(pgnFile.getPath());
        if (roundPgnInputStreamOptional.isPresent()) {
            try {
                Integer round = pgnFile.getRound();
                Long size = Long.valueOf(roundPgnInputStreamOptional.get().readAllBytes().length);
                String uri = "pgn://round/" + round;
                String name = "round" + round + "PgnFideWCC2024";
                String title = "PGN of the Round " + round + " game of the World Chess Championship";
                String description = title + " between Ding Liren and Gukesh Dommaraju";
                return new McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null);
            } catch (IOException e) {
                throw new ConfigurationException("unable to calculate the size of the resource");
            }
        }
        throw new ConfigurationException("unable find resource for path " + pgnFile.getPath());
    }
}
