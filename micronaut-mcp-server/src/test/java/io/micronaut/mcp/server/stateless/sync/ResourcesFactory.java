package io.micronaut.mcp.server.stateless.sync;

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

    ResourcesFactory(BeanContext beanContext, ResourceLoader resourceLoader, PgnLoader pgnLoader) {
        this.pgnLoader = pgnLoader;
        for (int round = 1; round <= 14; round++) {
            Optional<InputStream> roundPgnInputStreamOptional = resourceLoader.getResourceAsStream("classpath:fidewwc2024/round_" + round + ".pgn");
            if (roundPgnInputStreamOptional.isPresent()) {
                try {
                    Long size = Long.valueOf(roundPgnInputStreamOptional.get().readAllBytes().length);
                    String uri = "pgn://round/" + round;
                    String name = "round" + round + "PgnFideWCC2024";
                    String title = "PGN of the Round " + round + " game of the World Chess Championship";
                    String description = title + " between Ding Liren and Gukesh Dommaraju";
                    beanContext.registerSingleton(new McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null));
                } catch (IOException e) {
                    throw new ConfigurationException("unable to calculate the size of the resource");
                }
            }
        }
    }

    @EachBean(McpSchema.Resource.class)
    @Singleton
    McpStatelessServerFeatures.SyncResourceSpecification createPgnSyncResourceSpecification(McpSchema.Resource resource) {
        return new McpStatelessServerFeatures.SyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
            String uri = readResourceRequest.uri();
            int lastSlash = uri.lastIndexOf('/');
            String roundStr = uri.substring(lastSlash + 1);
            int round = Integer.parseInt(roundStr);
            List<McpSchema.ResourceContents> contents = new ArrayList<>();
            pgnLoader.loadPgn(round).ifPresent(text ->
                contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text)));
            return new McpSchema.ReadResourceResult(contents);
        });
    }
}
