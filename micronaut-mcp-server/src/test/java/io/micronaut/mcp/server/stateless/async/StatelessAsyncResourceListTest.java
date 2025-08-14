package io.micronaut.mcp.server.stateless.async;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST_TEMPLATES;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_TEMPLATES_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.info.name", value="world-chess-championship-2024-pgn")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.type", value = "STATELESS_ASYNC")
@Property(name = "spec.name", value = "StatelessAsyncResourceListTest")
@MicronautTest
class StatelessAsyncResourceListTest {
    @Test
    void testResourceRead(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_READ);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ, jsonRpc, true);
    }

    @Test
    void testResourcesTemplates(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_TEMPLATES, jsonRpc, true);

    }

    @Test
    void testResourceList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String input = RESOURCES_LIST;
        HttpRequest<?> req = HttpRequest.POST("/mcp", input);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST, jsonRpc, true);
    }

    @Requires(property = "spec.name", value = "StatelessAsyncResourceListTest")
    @Factory
    static class ResourcesFactory {
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

        @Singleton
        McpSchema.ResourceTemplate createPgnResourceTemplate() {
            String uriTemplate = "pgn://round/{round}";
            String name = "2024ChessChampionshipRoundPgn";
            String title = "PGN of a round World Chess Championship 2024";
            String description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju";
            return new McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null);
        }

        @EachBean(McpSchema.Resource.class)
        @Singleton
        McpStatelessServerFeatures.AsyncResourceSpecification createPgnSyncResourceSpecification(McpSchema.Resource resource) {
            return new McpStatelessServerFeatures.AsyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
                String uri = readResourceRequest.uri();
                int lastSlash = uri.lastIndexOf('/');
                String roundStr = uri.substring(lastSlash + 1);
                int round = Integer.parseInt(roundStr);
                List<McpSchema.ResourceContents> contents = new ArrayList<>();
                pgnLoader.loadPgn(round).ifPresent(text ->
                    contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text)));
                return Mono.just(new McpSchema.ReadResourceResult(contents));
            });
        }
    }
}
