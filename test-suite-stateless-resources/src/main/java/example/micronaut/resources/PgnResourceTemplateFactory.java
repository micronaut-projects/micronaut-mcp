package example.micronaut.resources;

import example.micronaut.utils.PgnLoader;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Factory
public class PgnResourceTemplateFactory {
    public static final String PGN_MIME_TYPE = "application/x-chess-pgn";

    private final PgnLoader pgnLoader;

    public PgnResourceTemplateFactory(BeanContext beanContext, ResourceLoader resourceLoader,
                                      PgnLoader pgnLoader) {
        this.pgnLoader = pgnLoader;
        int bytesLength = 0;
        List<String> pgns = new ArrayList<>();
        for (int round = 1; round <= 14; round++) {
            Optional<InputStream> roundPgnInputStreamOptional = resourceLoader.getResourceAsStream("classpath:fidewwc2024/round_" + round + ".pgn");
            if (roundPgnInputStreamOptional.isPresent()) {
                try {
                    byte[] bytes = roundPgnInputStreamOptional.get().readAllBytes();
                    String pgn = new String(bytes, StandardCharsets.UTF_8);
                    pgns.add(pgn);
                    bytesLength += bytes.length;
                    Long size = Long.valueOf(bytes.length);
                    String uri = "pgn://round/" + round;
                    String name = "round" + round + "PgnFideWCC2024";
                    String title = "PGN of the Round " + round + " game of the World Chess Championship";
                    String description = title + " between Ding Liren and Gukesh Dommaraju";
                    McpSchema.Resource resource = new McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null);
                    beanContext.registerSingleton(new McpStatelessServerFeatures.SyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
                        List<McpSchema.ResourceContents> contents = new ArrayList<>();
                        contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, pgn));
                        return new McpSchema.ReadResourceResult(contents);
                    }));

                } catch (IOException e) {
                    throw new ConfigurationException("unable to calculate the size of the resource");
                }
            }
        }
        String png = String.join("\n", pgns);
        String uri = "pgn://wcc2024";
        String name = "everyGamePgnFideWCC2024";
        String title = "PGNs for every Game of the 2024 World Chess Championship";
        String description = title + " between Ding Liren and Gukesh Dommaraju";
        Long size = Long.valueOf(bytesLength);
        McpSchema.Resource resource = new McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null);
        beanContext.registerSingleton(new McpStatelessServerFeatures.SyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
            List<McpSchema.ResourceContents> contents = new ArrayList<>();
            contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, png));
            return new McpSchema.ReadResourceResult(contents);
        }));
    }

    private Optional<String> pgn(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        String roundStr = uri.substring(lastSlash + 1);
        int round = Integer.parseInt(roundStr);
        return pgnLoader.loadPgn(round);
    }

    @Singleton
    McpSchema.ResourceTemplate createPgnResourceTemplate() {
        String uriTemplate = "pgn://round/{round}";
        String name = "chessChampionship2024RoundPgn";
        String title = "PGN of a round World Chess Championship 2024";
        String description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju";
        return new McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null);
    }

}
